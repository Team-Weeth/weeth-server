package com.weeth.domain.account.application.usecase.query

import com.weeth.domain.account.application.dto.request.AccountPaymentStatusFilter
import com.weeth.domain.account.application.dto.response.AccountPaymentStatusResponse
import com.weeth.domain.account.application.dto.response.AccountPaymentTargetResponse
import com.weeth.domain.account.application.dto.response.AccountPaymentTargetsResponse
import com.weeth.domain.account.application.dto.response.BankAccountResponse
import com.weeth.domain.account.application.exception.AccountNotFoundException
import com.weeth.domain.account.application.mapper.AccountPaymentTargetMapper
import com.weeth.domain.account.application.usecase.validateOwnedBy
import com.weeth.domain.account.domain.entity.Account
import com.weeth.domain.account.domain.entity.AccountPaymentTarget
import com.weeth.domain.account.domain.enums.AccountPaymentStatus
import com.weeth.domain.account.domain.enums.AccountTargetStatus
import com.weeth.domain.account.domain.repository.AccountPaymentTargetRepository
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.global.common.response.PageResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetAccountPaymentTargetQueryService(
    private val accountRepository: AccountRepository,
    private val paymentTargetRepository: AccountPaymentTargetRepository,
    private val clubMemberReader: ClubMemberReader,
    private val clubPermissionPolicy: ClubPermissionPolicy,
    private val accountPaymentTargetMapper: AccountPaymentTargetMapper,
) {
    fun findTargets(
        clubId: Long,
        accountId: Long,
        userId: Long,
        page: Int,
        size: Int,
        keyword: String?,
        targetStatus: AccountTargetStatus?,
    ): AccountPaymentTargetsResponse {
        val account = requireAdminAndGetAccount(clubId, accountId, userId)

        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))
        val normalizedKeyword = keyword?.trim()?.takeIf { it.isNotBlank() }
        // 납부 대상 후보는 동아리 전체가 아니라 해당 장부의 기수 명부로 한정한다.
        val cardinalNumber = account.cardinal
        val totalCount = clubMemberReader.countActiveByClubIdAndCardinalNumber(clubId, cardinalNumber)
        val targetedCount =
            paymentTargetRepository.countActiveClubMemberTargetsByAccountIdAndTargetStatus(
                accountId = accountId,
                targetStatus = AccountTargetStatus.TARGETED,
            )

        val targets =
            when (targetStatus) {
                AccountTargetStatus.TARGETED -> {
                    paymentTargetRepository
                        .findAllActiveClubMemberTargetsByAccountIdAndTargetStatus(
                            accountId = accountId,
                            targetStatus = AccountTargetStatus.TARGETED,
                            keyword = normalizedKeyword,
                            pageable = pageable,
                        ).map { accountPaymentTargetMapper.toResponse(it) }
                }

                AccountTargetStatus.EXCLUDED -> {
                    clubMemberReader
                        .findExcludedPaymentTargetCandidatesByCardinal(
                            clubId = clubId,
                            cardinalNumber = cardinalNumber,
                            accountId = accountId,
                            keyword = normalizedKeyword,
                            pageable = pageable,
                        ).mapWithSavedTargets(accountId)
                }

                null -> {
                    clubMemberReader
                        .findActiveByClubIdAndCardinalNumberAndKeyword(
                            clubId = clubId,
                            cardinalNumber = cardinalNumber,
                            keyword = normalizedKeyword,
                            pageable = pageable,
                        ).mapWithSavedTargets(accountId)
                }
            }

        return AccountPaymentTargetsResponse(
            summary =
                AccountPaymentTargetsResponse.PaymentTargetSummaryResponse(
                    totalCount = totalCount.toInt(),
                    targetedCount = targetedCount.toInt(),
                    excludedCount = (totalCount - targetedCount).coerceAtLeast(0).toInt(),
                ),
            targets = PageResponse.from(targets),
        )
    }

    /**
     * 운영 "회비관리" 페이지의 부원별 납부현황. findTargets(등록 플로우)와 진입 가드·행 매핑을 공유하되,
     * 납부 대상(TARGETED)만 납부 상태 필터·미납 순 정렬로 조회하고 상단 요약(수납액/목표/납부율/카운트/계좌)을 함께 만든다.
     */
    fun findPaymentStatus(
        clubId: Long,
        accountId: Long,
        userId: Long,
        paymentStatusFilter: AccountPaymentStatusFilter,
        keyword: String?,
        page: Int,
        size: Int,
    ): AccountPaymentStatusResponse {
        val account = requireAdminAndGetAccount(clubId, accountId, userId)

        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))
        val normalizedKeyword = keyword?.trim()?.takeIf { it.isNotBlank() }

        val members =
            if (paymentStatusFilter.isExcluded) {
                // 제외는 행이 아니라 '명부 − 활성 대상'으로 파생되므로(행이 없는 미선택 부원 포함) 명부 기반으로 조회한다.
                clubMemberReader
                    .findExcludedPaymentTargetCandidatesByCardinal(
                        clubId = clubId,
                        cardinalNumber = account.cardinal,
                        accountId = accountId,
                        keyword = normalizedKeyword,
                        pageable = pageable,
                    ).mapWithSavedTargets(accountId)
            } else {
                paymentTargetRepository
                    .findActiveTargetsByPaymentStatusOrderByUnpaidFirst(
                        accountId = accountId,
                        paymentStatus = paymentStatusFilter.toDomainOrNull(),
                        keyword = normalizedKeyword,
                        pageable = pageable,
                    ).map { accountPaymentTargetMapper.toResponse(it) }
            }

        return AccountPaymentStatusResponse(
            summary = buildPaymentStatusSummary(clubId, account),
            members = PageResponse.from(members),
        )
    }

    private fun buildPaymentStatusSummary(
        clubId: Long,
        account: Account,
    ): AccountPaymentStatusResponse.PaymentStatusSummaryResponse {
        val accountId = account.id
        val collectedAmount = paymentTargetRepository.sumPaidAmountByAccountId(accountId).toInt()
        val targetAmount = paymentTargetRepository.sumDueAmountByAccountId(accountId).toInt()
        val targetCount =
            paymentTargetRepository.countByAccountIdAndTargetStatus(accountId, AccountTargetStatus.TARGETED)
        val paidCount =
            paymentTargetRepository.countByAccountIdAndTargetStatusAndPaymentStatus(
                accountId,
                AccountTargetStatus.TARGETED,
                AccountPaymentStatus.PAID,
            )
        val unpaidCount =
            paymentTargetRepository.countByAccountIdAndTargetStatusAndPaymentStatus(
                accountId,
                AccountTargetStatus.TARGETED,
                AccountPaymentStatus.UNPAID,
            )
        val refundedCount =
            paymentTargetRepository.countByAccountIdAndTargetStatusAndPaymentStatus(
                accountId,
                AccountTargetStatus.TARGETED,
                AccountPaymentStatus.REFUNDED,
            )
        // 제외 카운트는 findTargets 와 동일하게 '활성 명부 − 활성 TARGETED' 로 파생한다(제외 행 부재 부원 포함).
        val activeTargetedCount =
            paymentTargetRepository.countActiveClubMemberTargetsByAccountIdAndTargetStatus(
                accountId = accountId,
                targetStatus = AccountTargetStatus.TARGETED,
            )
        val totalRosterCount = clubMemberReader.countActiveByClubIdAndCardinalNumber(clubId, account.cardinal)
        val excludedCount = (totalRosterCount - activeTargetedCount).coerceAtLeast(0)

        return AccountPaymentStatusResponse.PaymentStatusSummaryResponse(
            collectedAmount = collectedAmount,
            targetAmount = targetAmount,
            paymentRate =
                if (targetAmount >
                    0
                ) {
                    (collectedAmount.toDouble() / targetAmount).coerceIn(0.0, 1.0)
                } else {
                    null
                },
            targetCount = targetCount.toInt(),
            paidCount = paidCount.toInt(),
            unpaidCount = unpaidCount.toInt(),
            refundedCount = refundedCount.toInt(),
            excludedCount = excludedCount.toInt(),
            bankAccountPublic = account.bankAccountVisible,
            bankAccount = BankAccountResponse.from(account.bankAccount),
        )
    }

    private fun requireAdminAndGetAccount(
        clubId: Long,
        accountId: Long,
        userId: Long,
    ): Account {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        val account = accountRepository.findById(accountId).orElseThrow { AccountNotFoundException() }
        account.validateOwnedBy(clubId)
        return account
    }

    private fun Page<ClubMember>.mapWithSavedTargets(accountId: Long): Page<AccountPaymentTargetResponse> {
        val targetByClubMemberId = findTargetsByClubMemberId(accountId, content)
        return map { clubMember ->
            accountPaymentTargetMapper.toResponse(clubMember, targetByClubMemberId[clubMember.id])
        }
    }

    private fun findTargetsByClubMemberId(
        accountId: Long,
        clubMembers: List<ClubMember>,
    ): Map<Long, AccountPaymentTarget> {
        val clubMemberIds = clubMembers.map { it.id }
        if (clubMemberIds.isEmpty()) return emptyMap()

        return paymentTargetRepository
            .findAllByAccountIdAndClubMemberIdIn(accountId, clubMemberIds)
            .associateBy { it.clubMember.id }
    }
}
