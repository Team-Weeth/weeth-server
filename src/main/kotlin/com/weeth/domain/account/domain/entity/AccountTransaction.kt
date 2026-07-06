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
    type: AccountTransactionType,
    title: String,
    source: String?,
    amount: Money,
    transactedAt: LocalDateTime,
    category: String? = null,
    memo: String? = null,
    registeredByName: String? = null,
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
    var type: AccountTransactionType = type
        private set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var direction: AccountTransactionDirection = type.direction
        private set

    @Column(nullable = false, length = 100)
    var title: String = normalizeRequired(title, "거래 내용", MAX_TITLE_LENGTH)
        private set

    @Column(length = 50)
    var source: String? = normalizeOptional(source, "거래 출처", MAX_SOURCE_LENGTH)
        private set

    @Column(nullable = false)
    var amount: Int = amount.value
        private set

    @Column(name = "transacted_at", nullable = false)
    var transactedAt: LocalDateTime = transactedAt
        private set

    // 거래가 적용된 시점의 장부 총잔액 스냅샷(은행 거래내역의 잔액 열).
    // Account.applyTransaction 이 잔액 갱신 직후 기록한다. 이후 거래의 수정/삭제로는 재계산하지 않는다.
    @Column(name = "balance_after", nullable = false)
    var balanceAfter: Int = 0
        private set

    // MVP UI에서는 노출하지 않지만 분류/통계 확장을 위해 선반영해두는 자유 입력 카테고리.
    // 추후 카테고리 테이블로 승격될 수 있다 (membership-fee-domain-plan.md 참조).
    @Column(length = 30)
    var category: String? = normalizeOptional(category, "카테고리", MAX_CATEGORY_LENGTH)
        private set

    @Column(length = 200)
    var memo: String? = normalizeOptional(memo, "메모", MAX_MEMO_LENGTH)
        private set

    @Column(name = "registered_by_name", length = 50)
    var registeredByName: String? = normalizeOptional(registeredByName, "등록자 이름", MAX_REGISTERED_BY_NAME_LENGTH)
        private set

    var deletedAt: LocalDateTime? = null
        private set

    @Column(name = "is_applied", nullable = false)
    var isApplied: Boolean = false
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

    fun update(
        type: AccountTransactionType? = null,
        title: String? = null,
        source: String? = null,
        amount: Money? = null,
        transactedAt: LocalDateTime? = null,
        category: String? = null,
        memo: String? = null,
    ) {
        check(!isApplied) { "반영된 거래는 되돌린 뒤 수정할 수 있습니다." }
        check(deletedAt == null) { "삭제된 거래는 수정할 수 없습니다." }
        type?.let {
            this.type = it
            this.direction = it.direction
        }
        title?.let { this.title = normalizeRequired(it, "거래 내용", MAX_TITLE_LENGTH) }
        source?.let { this.source = normalizeOptional(it, "거래 출처", MAX_SOURCE_LENGTH) }
        amount?.let {
            require(it.value > 0) { "거래 금액은 0보다 커야 합니다: ${it.value}" }
            this.amount = it.value
        }
        transactedAt?.let { this.transactedAt = it }
        category?.let { this.category = normalizeOptional(it, "카테고리", MAX_CATEGORY_LENGTH) }
        memo?.let { this.memo = normalizeOptional(it, "메모", MAX_MEMO_LENGTH) }
    }

    internal fun markApplied() {
        check(!isApplied) { "이미 반영된 거래입니다." }
        check(deletedAt == null) { "삭제된 거래는 반영할 수 없습니다." }
        isApplied = true
    }

    /** 거래 적용 시점의 장부 총잔액을 기록한다. [Account.applyTransaction] 에서만 호출한다. */
    internal fun recordBalanceAfter(balance: Int) {
        balanceAfter = balance
    }

    internal fun markReverted() {
        check(isApplied) { "반영되지 않은 거래는 되돌릴 수 없습니다." }
        check(deletedAt == null) { "삭제된 거래는 되돌릴 수 없습니다." }
        isApplied = false
    }

    companion object {
        private const val MAX_TITLE_LENGTH = 100
        private const val MAX_SOURCE_LENGTH = 50
        private const val MAX_CATEGORY_LENGTH = 30
        private const val MAX_MEMO_LENGTH = 200
        private const val MAX_REGISTERED_BY_NAME_LENGTH = 50

        fun create(
            account: Account,
            type: AccountTransactionType,
            title: String,
            source: String?,
            amount: Money,
            transactedAt: LocalDateTime,
            category: String? = null,
            memo: String? = null,
            registeredByName: String? = null,
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
                registeredByName = registeredByName,
                paymentTarget = paymentTarget,
            )

        private fun normalizeRequired(
            value: String,
            fieldName: String,
            maxLength: Int,
        ): String {
            val normalized = value.trim()
            require(normalized.isNotBlank()) { "$fieldName 은 비어 있을 수 없습니다." }
            require(normalized.length <= maxLength) { "$fieldName 은 ${maxLength}자를 초과할 수 없습니다." }
            return normalized
        }

        private fun normalizeOptional(
            value: String?,
            fieldName: String,
            maxLength: Int,
        ): String? =
            value
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.also { require(it.length <= maxLength) { "$fieldName 은 ${maxLength}자를 초과할 수 없습니다." } }

        private fun AccountPaymentTarget.belongsTo(account: Account): Boolean =
            when {
                this.account === account -> true
                this.account.id != 0L && account.id != 0L -> this.account.id == account.id
                else -> false
            }
    }
}
