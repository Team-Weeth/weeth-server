package com.weeth.domain.account.application.usecase.query

import com.weeth.domain.account.application.dto.response.AccountPaymentTargetResponse
import com.weeth.domain.account.application.dto.response.AccountPaymentTargetsResponse
import com.weeth.domain.account.application.exception.AccountNotFoundException
import com.weeth.domain.account.application.mapper.AccountPaymentTargetMapper
import com.weeth.domain.account.domain.entity.AccountPaymentTarget
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
        clubPermissionPolicy.requireAdmin(clubId, userId)
        val account = accountRepository.findById(accountId).orElseThrow { AccountNotFoundException() }
        if (account.club.id != 0L && account.club.id != clubId) throw AccountNotFoundException()

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
