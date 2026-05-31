package com.weeth.domain.account.domain.entity

import com.weeth.domain.account.domain.enums.AccountTransactionDirection
import com.weeth.domain.account.domain.enums.AccountTransactionType
import com.weeth.domain.account.domain.vo.Money
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
import java.time.LocalDateTime

@Entity
@Table(
    name = "account_transaction",
    indexes = [
        Index(
            name = "idx_account_transaction_account_type_transacted_id",
            columnList = "account_id, type, transacted_at DESC, account_transaction_id DESC",
        ),
        Index(
            name = "idx_account_transaction_account_transacted_id",
            columnList = "account_id, transacted_at DESC, account_transaction_id DESC",
        ),
    ],
)
class AccountTransaction(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    val account: Account,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val type: AccountTransactionType,
    title: String,
    source: String?,
    amount: Money,
    @Column(name = "transacted_at", nullable = false)
    val transactedAt: LocalDateTime,
    category: String? = null,
    memo: String? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_payment_target_id")
    val paymentTarget: AccountPaymentTarget? = null,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_transaction_id")
    var id: Long = 0L
        private set

    // 인덱스/집계 쿼리에서 활용하기 위해 type 으로부터 파생된 방향을 함께 저장합니다.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val direction: AccountTransactionDirection = type.direction

    @Column(nullable = false, length = 100)
    var title: String = normalizeRequired(title, "거래 내용")
        private set

    @Column(length = 50)
    var source: String? = normalizeOptional(source)
        private set

    @Column(nullable = false)
    var amount: Int = amount.value
        private set

    // MVP UI에서는 노출하지 않지만 분류/통계 확장을 위해 선반영해두는 자유 입력 카테고리.
    // 추후 카테고리 테이블로 승격될 수 있다 (membership-fee-domain-plan.md 참조).
    @Column(length = 30)
    var category: String? = normalizeOptional(category)
        private set

    @Column(length = 200)
    var memo: String? = normalizeOptional(memo)
        private set

    var deletedAt: LocalDateTime? = null
        private set

    init {
        require(amount.value > 0) { "거래 금액은 0보다 커야 합니다: ${amount.value}" }
        paymentTarget?.let {
            check(it.belongsTo(account)) { "납부 대상이 거래 장부에 속하지 않습니다." }
        }
    }

    fun softDelete(deletedAt: LocalDateTime = LocalDateTime.now()) {
        if (this.deletedAt == null) {
            this.deletedAt = deletedAt
        }
    }

    companion object {
        fun create(
            account: Account,
            type: AccountTransactionType,
            title: String,
            source: String?,
            amount: Money,
            transactedAt: LocalDateTime,
            category: String? = null,
            memo: String? = null,
            paymentTarget: AccountPaymentTarget? = null,
        ): AccountTransaction =
            AccountTransaction(
                account = account,
                type = type,
                title = title,
                source = source,
                amount = amount,
                transactedAt = transactedAt,
                category = category,
                memo = memo,
                paymentTarget = paymentTarget,
            )

        private fun normalizeRequired(
            value: String,
            fieldName: String,
        ): String {
            val normalized = value.trim()
            require(normalized.isNotBlank()) { "$fieldName 은 비어 있을 수 없습니다." }
            return normalized
        }

        private fun normalizeOptional(value: String?): String? = value?.trim()?.takeIf { it.isNotBlank() }

        private fun AccountPaymentTarget.belongsTo(account: Account): Boolean =
            when {
                this.account === account -> true
                this.account.id != 0L && account.id != 0L -> this.account.id == account.id
                else -> false
            }
    }
}
