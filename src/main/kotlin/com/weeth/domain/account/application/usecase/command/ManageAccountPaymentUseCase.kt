package com.weeth.domain.account.application.usecase.command

import com.weeth.domain.account.application.dto.request.MarkPaymentPaidRequest
import com.weeth.domain.account.application.dto.request.MarkPaymentUnpaidRequest
import com.weeth.domain.account.application.dto.request.RefundPaymentRequest
import com.weeth.domain.account.application.exception.AccountNotActiveException
import com.weeth.domain.account.application.exception.AccountNotFoundException
import com.weeth.domain.account.application.exception.AccountPaymentNotRefundableException
import com.weeth.domain.account.application.exception.AccountPaymentTargetNotFoundException
import com.weeth.domain.account.application.usecase.validateOwnedBy
import com.weeth.domain.account.domain.entity.Account
import com.weeth.domain.account.domain.entity.AccountPaymentTarget
import com.weeth.domain.account.domain.entity.AccountTransaction
import com.weeth.domain.account.domain.enums.AccountPaymentStatus
import com.weeth.domain.account.domain.enums.AccountStatus
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

/**
 * TODO: 관리자가 납부 현황을 관리하는 경우 해당 거래 내역을 자동으로 생성한다. -> 환불/납부는 돈이 실제로 거래가 되므로 대시보드에서 확인이 가능해야함
 * TODO: 대신 환불/납부 거래내역은 관리자만 볼 수 있어야한다.
 * TODO: 유저 사이드에서 납부 거래내역은 하나의 "회비" 거래내역에 누적이 되어야한다.
 * 즉 현황을 업데이트할 때 해당 인원만큼 개별로 거래 내역이 생성되며, 이는 관리자만 확인이 가능하고, 유저사이드에서는 하나의 거래 내역에 금액이 +- 되는 방식으로 동작한다.
 * 환불의 경우는 유저사이드에서도 확인이 가능하되, 이름은 가린다.
 */
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
        val account = getActiveAccountWithLock(clubId, accountId, userId)
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
        val account = getActiveAccountWithLock(clubId, accountId, userId)
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

    /** 환불(벌크): DUES는 보존하고 REFUND 지출 거래를 생성해 잔액을 차감하며 상태를 REFUNDED로 전이한다. */
    @Transactional
    fun refund(
        clubId: Long,
        accountId: Long,
        request: RefundPaymentRequest,
        userId: Long,
    ) {
        val account = getActiveAccountWithLock(clubId, accountId, userId)
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
        userId: Long,
    ): Account {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        val account = accountRepository.findByIdWithLock(accountId) ?: throw AccountNotFoundException()
        account.validateOwnedBy(clubId)
        if (account.status != AccountStatus.ACTIVE) throw AccountNotActiveException()
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
