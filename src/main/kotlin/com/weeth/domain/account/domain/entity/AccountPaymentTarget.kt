package com.weeth.domain.account.domain.entity

import com.weeth.domain.account.domain.enums.AccountPaymentStatus
import com.weeth.domain.account.domain.enums.AccountTargetStatus
import com.weeth.domain.account.domain.vo.Money
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "account_payment_target",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_account_payment_target_account_member",
            columnNames = ["account_id", "club_member_id"],
        ),
    ],
    indexes = [
        Index(
            name = "idx_account_payment_target_account_status",
            columnList = "account_id, target_status, payment_status",
        ),
    ],
)
class AccountPaymentTarget(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    val account: Account,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_member_id", nullable = false)
    val clubMember: ClubMember,
    targetStatus: AccountTargetStatus,
    dueAmount: Money,
    memo: String? = null,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_payment_target_id")
    var id: Long = 0L
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "target_status", nullable = false, length = 20)
    var targetStatus: AccountTargetStatus = targetStatus
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    var paymentStatus: AccountPaymentStatus = AccountPaymentStatus.UNPAID
        private set

    @Column(nullable = false)
    var dueAmount: Int = dueAmount.value
        private set

    @Column(nullable = false)
    var paidAmount: Int = 0
        private set

    var paidAt: LocalDateTime? = null
        private set

    var confirmedBy: Long? = null
        private set

    @Column(length = 200)
    var memo: String? = normalizeOptional(memo)
        private set

    init {
        if (targetStatus == AccountTargetStatus.TARGETED) {
            require(dueAmount.value > 0) { "납부 대상 금액은 0보다 커야 합니다: ${dueAmount.value}" }
        }
    }

    fun target(dueAmount: Money) {
        require(dueAmount.value > 0) { "납부 대상 금액은 0보다 커야 합니다: ${dueAmount.value}" }
        targetStatus = AccountTargetStatus.TARGETED
        paymentStatus = AccountPaymentStatus.UNPAID
        this.dueAmount = dueAmount.value
        paidAmount = 0
        paidAt = null
        confirmedBy = null
    }

    fun exclude() {
        targetStatus = AccountTargetStatus.EXCLUDED
        paymentStatus = AccountPaymentStatus.UNPAID
        dueAmount = 0
        paidAmount = 0
        paidAt = null
        confirmedBy = null
    }

    fun markPaid(
        amount: Money,
        confirmedBy: Long,
        paidAt: LocalDateTime,
    ) {
        check(targetStatus == AccountTargetStatus.TARGETED) { "납부 대상만 납부 완료 처리할 수 있습니다." }
        check(paymentStatus == AccountPaymentStatus.UNPAID) { "이미 납부 완료된 대상입니다." }
        require(amount.value > 0) { "납부 금액은 0보다 커야 합니다: ${amount.value}" }
        require(amount.value == dueAmount) {
            "납부 금액은 대상 금액과 일치해야 합니다. 대상: $dueAmount, 납부: ${amount.value}"
        }
        paymentStatus = AccountPaymentStatus.PAID
        paidAmount = amount.value
        this.confirmedBy = confirmedBy
        this.paidAt = paidAt
    }

    fun markUnpaid() {
        paymentStatus = AccountPaymentStatus.UNPAID
        paidAmount = 0
        paidAt = null
        confirmedBy = null
    }

    companion object {
        fun createTargeted(
            account: Account,
            clubMember: ClubMember,
            dueAmount: Money,
            memo: String? = null,
        ): AccountPaymentTarget =
            AccountPaymentTarget(
                account = account,
                clubMember = clubMember,
                targetStatus = AccountTargetStatus.TARGETED,
                dueAmount = dueAmount,
                memo = memo,
            )

        fun createExcluded(
            account: Account,
            clubMember: ClubMember,
            memo: String? = null,
        ): AccountPaymentTarget =
            AccountPaymentTarget(
                account = account,
                clubMember = clubMember,
                targetStatus = AccountTargetStatus.EXCLUDED,
                dueAmount = Money.ZERO,
                memo = memo,
            )

        private fun normalizeOptional(value: String?): String? = value?.trim()?.takeIf { it.isNotBlank() }
    }
}
