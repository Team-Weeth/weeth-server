package com.weeth.domain.account.application.usecase.command

import com.weeth.domain.account.application.dto.request.ExcludePaymentTargetsRequest
import com.weeth.domain.account.application.dto.request.MarkPaymentPaidRequest
import com.weeth.domain.account.application.dto.request.MarkPaymentUnpaidRequest
import com.weeth.domain.account.application.dto.request.RefundPaymentRequest
import com.weeth.domain.account.application.exception.AccountNotActiveException
import com.weeth.domain.account.application.exception.AccountNotFoundException
import com.weeth.domain.account.application.exception.AccountPaymentNotRefundableException
import com.weeth.domain.account.application.exception.AccountPaymentTargetNotFoundException
import com.weeth.domain.account.application.exception.AccountPaymentTargetPaidException
import com.weeth.domain.account.application.usecase.validateOwnedBy
import com.weeth.domain.account.domain.entity.Account
import com.weeth.domain.account.domain.entity.AccountPaymentTarget
import com.weeth.domain.account.domain.entity.AccountTransaction
import com.weeth.domain.account.domain.enums.AccountPaymentStatus
import com.weeth.domain.account.domain.enums.AccountTargetStatus
import com.weeth.domain.account.domain.enums.AccountTransactionType
import com.weeth.domain.account.domain.repository.AccountPaymentTargetRepository
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.account.domain.repository.AccountTransactionRepository
import com.weeth.domain.account.domain.vo.Money
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class ManageAccountPaymentUseCase(
    private val accountRepository: AccountRepository,
    private val paymentTargetRepository: AccountPaymentTargetRepository,
    private val transactionRepository: AccountTransactionRepository,
    private val clubPermissionPolicy: ClubPermissionPolicy,
    private val clock: Clock,
) {
    /** 납부 확인(벌크): 대상별 DUES 수입 거래를 생성하고 잔액을 가산한다. */
    @Transactional
    fun markPaid(
        clubId: Long,
        accountId: Long,
        request: MarkPaymentPaidRequest,
        userId: Long,
    ) {
        val admin = clubPermissionPolicy.requireAdmin(clubId, userId)
        val account = getActiveAccountWithLock(clubId, accountId)
        val targets = getTargets(accountId, request.targetIds)
        val paidAt = request.paidAt ?: LocalDateTime.now(clock)

        // 거래는 한 번에 saveAll, 잔액 가산(applyTransaction)은 누적 연산이라 건별로 적용한다.
        val duesTransactions =
            targets.map { target ->
                target.markPaid(Money.of(target.dueAmount), userId, paidAt)
                AccountTransaction
                    .create(
                        account = account,
                        type = AccountTransactionType.DUES,
                        title = "회비 납부",
                        source = target.memberName(),
                        amount = Money.of(target.dueAmount),
                        transactedAt = paidAt,
                        memo = request.memo,
                        registeredByName = admin.user.name,
                        paymentTarget = target,
                    ).also { account.applyTransaction(it) }
            }
        transactionRepository.saveAll(duesTransactions)
        account.markModifiedBy(userId)
    }

    /** 납부 정정(벌크): 대상의 활성 DUES 거래를 원복(soft delete)하고 미납으로 되돌린다. */
    @Transactional
    fun markUnpaid(
        clubId: Long,
        accountId: Long,
        request: MarkPaymentUnpaidRequest,
        userId: Long,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        val account = getActiveAccountWithLock(clubId, accountId)
        val targets = getTargets(accountId, request.targetIds)

        targets.forEach { target ->
            transactionRepository
                .findByPaymentTargetIdAndTypeAndDeletedAtIsNull(target.id, AccountTransactionType.DUES)
                ?.let { dues ->
                    account.revertTransaction(dues)
                    dues.softDelete()
                }
            target.markUnpaid()
        }
        account.markModifiedBy(userId)
    }

    /**
     * 납부 대상 제외(벌크): 선택된 대상들을 EXCLUDED 로 전이해 납부 대상에서 빼낸다.
     * 미납(UNPAID) 대상만 제외할 수 있다 — 납부(PAID)/환불(REFUNDED) 이력이 있으면 거래·잔액 정합성이 깨지므로
     * 하나라도 포함되면 아무것도 변경하지 않고 예외를 던진다(먼저 납부 정정 후 제외하도록 유도).
     */
    @Transactional
    fun exclude(
        clubId: Long,
        accountId: Long,
        request: ExcludePaymentTargetsRequest,
        userId: Long,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        val account = getActiveAccountWithLock(clubId, accountId)
        val targets = getTargets(accountId, request.targetIds)

        // 전부-또는-전무: 부분 적용을 막기 위해 전이 전에 먼저 전량 검증한다.
        if (targets.any {
                it.targetStatus != AccountTargetStatus.TARGETED ||
                    it.paymentStatus != AccountPaymentStatus.UNPAID
            }
        ) {
            throw AccountPaymentTargetPaidException()
        }

        targets.forEach { it.exclude() }
        account.markModifiedBy(userId)
    }

    /** 환불(벌크): DUES는 보존하고 REFUND 지출 거래를 생성해 잔액을 차감하며 상태를 REFUNDED로 전이한다. */
    @Transactional
    fun refund(
        clubId: Long,
        accountId: Long,
        request: RefundPaymentRequest,
        userId: Long,
    ) {
        val admin = clubPermissionPolicy.requireAdmin(clubId, userId)
        val account = getActiveAccountWithLock(clubId, accountId)
        val targets = getTargets(accountId, request.targetIds)
        val refundedAt = LocalDateTime.now(clock)

        val refundTransactions =
            targets.map { target ->
                if (target.paymentStatus != AccountPaymentStatus.PAID) throw AccountPaymentNotRefundableException()
                AccountTransaction
                    .create(
                        account = account,
                        type = AccountTransactionType.REFUND,
                        title = "회비 환불",
                        source = target.memberName(),
                        amount = Money.of(target.paidAmount),
                        transactedAt = refundedAt,
                        memo = request.memo,
                        registeredByName = admin.user.name,
                        paymentTarget = target,
                    ).also {
                        account.applyTransaction(it)
                        target.markRefunded(userId, refundedAt)
                    }
            }
        transactionRepository.saveAll(refundTransactions)
        account.markModifiedBy(userId)
    }

    private fun getActiveAccountWithLock(
        clubId: Long,
        accountId: Long,
    ): Account {
        val account = accountRepository.findByIdWithLock(accountId) ?: throw AccountNotFoundException()
        account.validateOwnedBy(clubId)
        if (!account.isActive) throw AccountNotActiveException()
        return account
    }

    private fun getTargets(
        accountId: Long,
        targetIds: List<Long>,
    ): List<AccountPaymentTarget> {
        val ids = targetIds.distinct()
        val targets = paymentTargetRepository.findAllByAccountIdAndIdIn(accountId, ids)
        if (targets.size != ids.size) throw AccountPaymentTargetNotFoundException()
        return targets
    }

    private fun AccountPaymentTarget.memberName(): String = clubMember.user.name
}
