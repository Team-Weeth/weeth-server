package com.weeth.domain.account.domain.entity

import com.weeth.domain.account.domain.enums.AccountRegistrationStep
import com.weeth.domain.account.domain.enums.AccountStatus
import com.weeth.domain.account.domain.enums.AccountTransactionDirection
import com.weeth.domain.account.domain.vo.BankAccount
import com.weeth.domain.account.domain.vo.Money
import com.weeth.domain.club.domain.entity.Club
import com.weeth.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_account_club_cardinal",
            columnNames = ["club_id", "cardinal"],
        ),
    ],
)
class Account(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    val club: Club,
    id: Long = 0,
    description: String? = null,
    totalAmount: Int,
    currentAmount: Int,
    cardinal: Int,
    name: String? = null,
    duesAmount: Int = 0,
    carryOverEnabled: Boolean = false,
    carryOverAmount: Int = 0,
    carryOverMemo: String? = null,
    currentBalance: Int = 0,
    bankAccount: BankAccount? = null,
    bankAccountVisible: Boolean = false, // 계좌 노출 여부
    memberVisible: Boolean = false, // 회비 회원 공개 여부
    lastModifiedBy: Long? = null, // 마지막 수정자. 추후 수정 로그 기능 확장에 따라 수정될 가능성 존재
    status: AccountStatus = AccountStatus.ACTIVE,
    registrationStep: AccountRegistrationStep = AccountRegistrationStep.BASIC,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    var id: Long = id
        private set

    @Column(nullable = true)
    var description: String? = description
        private set

    @Column(nullable = false)
    var totalAmount: Int = totalAmount
        private set

    // 레거시 Receipt 흐름의 잔액. currentBalance 와 같은 값을 가지며,
    // Receipt → AccountTransaction 마이그레이션 완료 시 currentBalance 로 통합 후 제거 예정.
    @Column(nullable = false)
    var currentAmount: Int = currentAmount
        private set

    @Column(nullable = false)
    var cardinal: Int = cardinal
        private set

    @Column(length = 100)
    var name: String? = name
        private set

    @Column(nullable = false)
    var duesAmount: Int = duesAmount
        private set

    @Column(nullable = false)
    var carryOverEnabled: Boolean = carryOverEnabled
        private set

    @Column(nullable = false)
    var carryOverAmount: Int = carryOverAmount
        private set

    @Column(length = 200)
    var carryOverMemo: String? = carryOverMemo
        private set

    // 신규 AccountTransaction 흐름의 실제 통장 잔액. currentAmount 와 같은 값을 유지하며,
    // Receipt 제거 이후 단일 잔액 필드로 남길 예정.
    @Column(nullable = false)
    var currentBalance: Int = currentBalance
        private set

    @Embedded
    var bankAccount: BankAccount? = bankAccount
        private set

    @Column(nullable = false)
    var bankAccountVisible: Boolean = bankAccountVisible
        private set

    @Column(nullable = false)
    var memberVisible: Boolean = memberVisible
        private set

    var lastModifiedBy: Long? = lastModifiedBy
        private set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: AccountStatus = status
        private set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var registrationStep: AccountRegistrationStep = registrationStep
        private set

    fun spend(amount: Money) {
        require(amount.value > 0) { "사용 금액은 0보다 커야 합니다: ${amount.value}" }
        check(currentAmount >= amount.value) { "잔액이 부족합니다. 현재: $currentAmount, 요청: ${amount.value}" }
        currentAmount -= amount.value
        currentBalance -= amount.value
    }

    fun cancelSpend(amount: Money) {
        require(amount.value > 0) { "취소 금액은 0보다 커야 합니다: ${amount.value}" }
        check(currentAmount + amount.value <= totalAmount) { "총액을 초과할 수 없습니다. 총액: $totalAmount" }
        currentAmount += amount.value
        currentBalance += amount.value
    }

    fun adjustSpend(
        oldAmount: Money,
        newAmount: Money,
    ) {
        cancelSpend(oldAmount)
        spend(newAmount)
    }

    fun updateBasicInfo(
        name: String,
        duesAmount: Money,
        description: String? = null,
    ) {
        val normalizedName = name.trim()
        val normalizedDescription = description?.trim()
        require(normalizedName.isNotBlank()) { "회비 이름은 비어 있을 수 없습니다." }
        require(duesAmount.value > 0) { "1인 회비 금액은 0보다 커야 합니다: ${duesAmount.value}" }
        this.name = normalizedName
        this.duesAmount = duesAmount.value
        this.description = normalizedDescription
        advanceRegistrationStep(AccountRegistrationStep.PAYMENT_TARGETS)
    }

    fun updateCarryOver(
        enabled: Boolean,
        amount: Money?,
        memo: String?,
    ) {
        val carryOver = if (enabled) requireNotNull(amount) { "이월 금액은 필수입니다." } else Money.ZERO
        require(carryOver.value >= 0) { "이월 금액은 0 이상이어야 합니다: ${carryOver.value}" }
        carryOverEnabled = enabled
        carryOverAmount = carryOver.value
        carryOverMemo = memo?.trim()?.takeIf { it.isNotBlank() }
        advanceRegistrationStep(AccountRegistrationStep.BANK_ACCOUNT)
    }

    fun updateBankAccount(
        bankAccount: BankAccount?,
        visible: Boolean,
    ) {
        if (visible) {
            requireNotNull(bankAccount) { "계좌 공개 시 계좌 정보는 필수입니다." }
        }
        this.bankAccount = bankAccount
        bankAccountVisible = visible
        advanceRegistrationStep(AccountRegistrationStep.REVIEW)
    }

    fun advanceRegistrationStep(next: AccountRegistrationStep) {
        if (status != AccountStatus.DRAFT) return
        if (next.isAfter(registrationStep)) {
            registrationStep = next
        }
    }

    fun showToMembers() {
        check(status == AccountStatus.ACTIVE) { "활성화된 회비 장부만 회원에게 공개할 수 있습니다." }
        memberVisible = true
    }

    fun hideFromMembers() {
        memberVisible = false
    }

    fun markModifiedBy(adminId: Long) {
        require(adminId > 0) { "마지막 수정자 ID는 0보다 커야 합니다: $adminId" }
        lastModifiedBy = adminId
    }

    fun activate() {
        check(status == AccountStatus.DRAFT) { "초안 상태의 회비 장부만 활성화할 수 있습니다." }
        check(!name.isNullOrBlank()) { "회비 이름은 필수입니다." }
        check(duesAmount > 0) { "1인 회비 금액은 필수입니다." }
        status = AccountStatus.ACTIVE
    }

    /**
     * 잔액을 갱신하므로 동시성 보호가 필요합니다.
     * 호출 측은 AccountRepository.findByIdWithLock 으로 조회한 인스턴스에 적용해야 합니다.
     *
     * currentBalance(신규)와 currentAmount(레거시 영수증 흐름)는 동일한 잔액을 의미하므로
     * 한 경로에서 다른 경로의 필드가 어긋나지 않도록 함께 갱신합니다.
     */
    fun applyTransaction(transaction: AccountTransaction) {
        check(transaction.belongsTo()) { "거래가 해당 장부에 속하지 않습니다." }
        check(transaction.deletedAt == null) { "삭제된 거래는 반영할 수 없습니다." }
        check(!transaction.isApplied) { "이미 반영된 거래입니다." }
        when (transaction.direction) {
            AccountTransactionDirection.INCOME -> {
                currentBalance += transaction.amount
                currentAmount += transaction.amount
            }

            AccountTransactionDirection.EXPENSE -> {
                check(currentBalance >= transaction.amount) {
                    "잔액이 부족합니다. 현재: $currentBalance, 요청: ${transaction.amount}"
                }
                currentBalance -= transaction.amount
                currentAmount -= transaction.amount
            }
        }
        transaction.markApplied()
    }

    /**
     * applyTransaction 과 동일하게 잔액을 갱신하므로 잠금 조회된 인스턴스에 적용해야 합니다.
     */
    fun revertTransaction(transaction: AccountTransaction) {
        check(transaction.belongsTo()) { "거래가 해당 장부에 속하지 않습니다." }
        check(transaction.isApplied) { "반영되지 않은 거래는 되돌릴 수 없습니다." }
        when (transaction.direction) {
            AccountTransactionDirection.INCOME -> {
                check(currentBalance >= transaction.amount) {
                    "잔액이 부족합니다. 현재: $currentBalance, 요청: ${transaction.amount}"
                }
                currentBalance -= transaction.amount
                currentAmount -= transaction.amount
            }

            AccountTransactionDirection.EXPENSE -> {
                currentBalance += transaction.amount
                currentAmount += transaction.amount
            }
        }
        transaction.markReverted()
    }

    companion object {
        @Deprecated(
            "레거시 데이터 호환용 팩토리입니다. 신규 회비 장부는 createDraft + updateBasicInfo 흐름을 사용하세요.",
            ReplaceWith("Account.createDraft(club, cardinal)"),
        )
        fun create(
            club: Club,
            description: String,
            totalAmount: Int,
            cardinal: Int,
        ): Account {
            require(totalAmount > 0) { "총액은 0보다 커야 합니다: $totalAmount" }
            return Account(
                club = club,
                description = description,
                totalAmount = totalAmount,
                currentAmount = totalAmount,
                cardinal = cardinal,
                name = description,
                duesAmount = totalAmount,
                currentBalance = totalAmount,
                status = AccountStatus.ACTIVE,
            )
        }

        fun createDraft(
            club: Club,
            cardinal: Int,
        ): Account {
            require(cardinal > 0) { "기수는 0보다 커야 합니다: $cardinal" }
            return Account(
                club = club,
                description = "",
                totalAmount = 0,
                currentAmount = 0,
                cardinal = cardinal,
                status = AccountStatus.DRAFT,
            )
        }
    }

    private fun AccountTransaction.belongsTo(): Boolean =
        when {
            this.account === this@Account -> true
            this.account.id != 0L && this@Account.id != 0L -> this.account.id == this@Account.id
            else -> false
        }
}
