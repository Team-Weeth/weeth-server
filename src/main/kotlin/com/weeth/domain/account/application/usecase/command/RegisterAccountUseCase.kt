package com.weeth.domain.account.application.usecase.command

import com.weeth.domain.account.application.dto.request.SaveAccountBankAccountRequest
import com.weeth.domain.account.application.dto.request.SaveAccountBasicRequest
import com.weeth.domain.account.application.dto.request.SaveAccountCarryOverRequest
import com.weeth.domain.account.application.dto.request.SavePaymentTargetsRequest
import com.weeth.domain.account.application.dto.response.CreateAccountDraftResponse
import com.weeth.domain.account.application.exception.AccountExistsException
import com.weeth.domain.account.application.exception.AccountInvalidDraftStateException
import com.weeth.domain.account.application.exception.AccountNotFoundException
import com.weeth.domain.account.application.exception.AccountPaymentTargetMemberInvalidException
import com.weeth.domain.account.application.exception.AccountPaymentTargetPaidException
import com.weeth.domain.account.domain.entity.Account
import com.weeth.domain.account.domain.entity.AccountPaymentTarget
import com.weeth.domain.account.domain.entity.AccountTransaction
import com.weeth.domain.account.domain.enums.AccountPaymentStatus
import com.weeth.domain.account.domain.enums.AccountRegistrationStep
import com.weeth.domain.account.domain.enums.AccountStatus
import com.weeth.domain.account.domain.enums.AccountTargetStatus
import com.weeth.domain.account.domain.enums.AccountTransactionType
import com.weeth.domain.account.domain.repository.AccountPaymentTargetRepository
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.account.domain.repository.AccountTransactionRepository
import com.weeth.domain.account.domain.vo.Money
import com.weeth.domain.cardinal.application.exception.CardinalNotFoundException
import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.repository.ClubMemberCardinalReader
import com.weeth.domain.club.domain.repository.ClubReader
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.user.domain.repository.UserReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class RegisterAccountUseCase(
    private val accountRepository: AccountRepository,
    private val paymentTargetRepository: AccountPaymentTargetRepository,
    private val transactionRepository: AccountTransactionRepository,
    private val cardinalReader: CardinalReader,
    private val clubReader: ClubReader,
    private val clubMemberCardinalReader: ClubMemberCardinalReader,
    private val clubPermissionPolicy: ClubPermissionPolicy,
    private val userReader: UserReader,
) {
    @Transactional
    fun createDraft(
        clubId: Long,
        cardinal: Int,
        userId: Long,
    ): CreateAccountDraftResponse {
        clubPermissionPolicy.requireAdmin(clubId, userId)

        accountRepository.findByClubIdAndCardinal(clubId, cardinal)?.let {
            if (it.status == AccountStatus.DRAFT) {
                return CreateAccountDraftResponse(
                    accountId = it.id,
                    isNew = false,
                    lastModifiedByName =
                        it.lastModifiedBy?.let { modifierId ->
                            userReader.findByIdOrNull(modifierId)?.name
                        },
                )
            }
            throw AccountExistsException()
        }

        cardinalReader.findByClubIdAndCardinalNumber(clubId, cardinal)
            ?: throw CardinalNotFoundException()

        val account =
            Account
                .createDraft(club = clubReader.getClubById(clubId), cardinal = cardinal)
                .also { it.markModifiedBy(userId) }

        return CreateAccountDraftResponse(
            accountId = accountRepository.save(account).id,
            isNew = true,
            lastModifiedByName = null,
        )
    }

    @Transactional
    fun discardDraft(
        clubId: Long,
        accountId: Long,
        userId: Long,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        val account = getAccountWithLock(clubId, accountId)

        if (account.status != AccountStatus.DRAFT) throw AccountInvalidDraftStateException()

        accountRepository.delete(account)
    }

    @Transactional
    fun saveBasic(
        clubId: Long,
        accountId: Long,
        request: SaveAccountBasicRequest,
        userId: Long,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        val account = getAccountWithLock(clubId, accountId)

        account.updateBasicInfo(
            name = request.name,
            duesAmount = Money.of(request.duesAmount),
            description = request.description,
        )

        account.markModifiedBy(userId)
    }

    @Transactional
    fun saveCarryOver(
        clubId: Long,
        accountId: Long,
        request: SaveAccountCarryOverRequest,
        userId: Long,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        val account = getAccountWithLock(clubId, accountId)

        account.updateCarryOver(
            enabled = request.enabled,
            amount = request.amount?.let(Money::of),
            memo = request.memo,
        )

        account.markModifiedBy(userId)
    }

    /**
     * 등록 완료 시 직전 활성 기수 장부에 남은 잔액을 지출 거래로 정리해 0원으로 만든다.
     * 실제 이월된 금액이 있으면 신규 장부로의 전출, 없으면 미이월 잔액 정리 명목으로 기록해
     * 같은 돈이 두 장부에 중복 집계되지 않도록 한다.
     * (CARRY_OVER 수입 거래와 같은 조건이므로 전출 기록이 있을 때만 대응하는 이월 수입이 존재한다)
     */
    private fun settlePreviousAccountBalance(
        clubId: Long,
        account: Account,
    ) {
        val previousAccount =
            accountRepository.findTopByClubIdAndCardinalLessThanAndStatusOrderByCardinalDesc(
                clubId = clubId,
                cardinal = account.cardinal,
                status = AccountStatus.ACTIVE,
            ) ?: return
        if (previousAccount.currentBalance <= 0) return

        val lockedPrevious = accountRepository.findByIdWithLock(previousAccount.id) ?: return
        if (lockedPrevious.currentBalance <= 0) return

        val (title, memo) =
            if (account.carryOverAmount > 0) {
                "이월 잔액 전출" to "${account.cardinal}기 회비로 이월되어 자동 지출 처리되었습니다."
            } else {
                "미이월 잔액 정리" to "${account.cardinal}기 회비 등록 시 이월하지 않기를 선택하여 자동 지출 처리되었습니다."
            }

        val expense =
            AccountTransaction.create(
                account = lockedPrevious,
                type = AccountTransactionType.EXPENSE,
                title = title,
                source = null,
                amount = Money.of(lockedPrevious.currentBalance),
                transactedAt = LocalDateTime.now(),
                memo = memo,
            )

        transactionRepository.save(expense)
        lockedPrevious.applyTransaction(expense)
    }

    /**
     * 회비 납부 대상을 델타 방식으로 저장한다. 요청에 포함된 멤버만 갱신하며,
     * 두 목록에 모두 없는 멤버(비활성 멤버 포함)의 기존 상태는 건드리지 않는다.
     *
     * 후보는 동아리 전체가 아니라 해당 장부의 기수 활성 명부로 한정한다.
     * - targetedClubMemberIds: 납부 대상으로 갱신(이미 납부 완료된 대상은 유지), 행이 없으면 신규 생성
     * - excludedClubMemberIds: 제외 처리(납부 완료된 대상은 제외 불가), 행이 없으면 이미 제외 상태이므로 건너뜀
     */
    @Transactional
    fun savePaymentTargets(
        clubId: Long,
        accountId: Long,
        request: SavePaymentTargetsRequest,
        userId: Long,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        val account = getAccountWithLock(clubId, accountId)
        val targetedMemberIds = request.targetedClubMemberIds.distinct().sorted()
        val excludedMemberIds = request.excludedClubMemberIds.distinct().sorted()
        if (targetedMemberIds.intersect(excludedMemberIds.toSet()).isNotEmpty()) {
            throw AccountPaymentTargetMemberInvalidException()
        }

        val requestedMemberIds = targetedMemberIds + excludedMemberIds
        if (requestedMemberIds.isNotEmpty()) {
            val rosterById =
                clubMemberCardinalReader
                    .findAllByClubIdAndCardinalNumber(clubId, account.cardinal, MemberStatus.ACTIVE)
                    .associate { it.clubMember.id to it.clubMember }
            if (!rosterById.keys.containsAll(requestedMemberIds)) throw AccountPaymentTargetMemberInvalidException()

            val dueAmount = Money.of(account.duesAmount)
            val existingByMemberId =
                paymentTargetRepository
                    .findAllByAccountIdAndClubMemberIdIn(accountId, requestedMemberIds)
                    .associateBy { it.clubMember.id }

            excludedMemberIds.mapNotNull { existingByMemberId[it] }.forEach { target ->
                if (target.paymentStatus != AccountPaymentStatus.UNPAID) throw AccountPaymentTargetPaidException()
                target.exclude()
            }

            targetedMemberIds.mapNotNull { existingByMemberId[it] }.forEach { target ->
                val alreadyPaid =
                    target.targetStatus == AccountTargetStatus.TARGETED &&
                        target.paymentStatus == AccountPaymentStatus.PAID
                if (!alreadyPaid) {
                    target.target(dueAmount)
                }
            }

            val newTargets =
                targetedMemberIds
                    .filterNot { existingByMemberId.containsKey(it) }
                    .map { AccountPaymentTarget.createTargeted(account, rosterById.getValue(it), dueAmount) }

            if (newTargets.isNotEmpty()) {
                paymentTargetRepository.saveAll(newTargets)
            }
        }

        account.advanceRegistrationStep(AccountRegistrationStep.CARRY_OVER)
        account.markModifiedBy(userId)
    }

    private fun getAccountWithLock(
        clubId: Long,
        accountId: Long,
    ): Account {
        val account = accountRepository.findByIdWithLock(accountId) ?: throw AccountNotFoundException()
        if (account.club.id != 0L && account.club.id != clubId) throw AccountNotFoundException()
        return account
    }
}
