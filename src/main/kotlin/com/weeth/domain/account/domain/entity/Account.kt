package com.weeth.domain.account.domain.entity

import com.weeth.domain.account.domain.vo.Money
import com.weeth.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class Account(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    val id: Long = 0,
    @Column(nullable = false)
    val description: String,
    @Column(nullable = false)
    val totalAmount: Int,
    @Column(nullable = false)
    var currentAmount: Int,
    @Column(nullable = false)
    val cardinal: Int,
) : BaseEntity() {
    fun spend(amount: Money) {
        require(amount.value > 0) { "사용 금액은 0보다 커야 합니다: ${amount.value}" }
        check(currentAmount >= amount.value) { "잔액이 부족합니다. 현재: $currentAmount, 요청: ${amount.value}" }
        currentAmount -= amount.value
    }

    fun cancelSpend(amount: Money) {
        require(amount.value > 0) { "취소 금액은 0보다 커야 합니다: ${amount.value}" }
        check(currentAmount + amount.value <= totalAmount) { "총액을 초과할 수 없습니다. 총액: $totalAmount" }
        currentAmount += amount.value
    }

    fun adjustSpend(
        oldAmount: Money,
        newAmount: Money,
    ) {
        cancelSpend(oldAmount)
        spend(newAmount)
    }

    companion object {
        @JvmStatic
        fun create(
            description: String,
            totalAmount: Int,
            cardinal: Int,
        ): Account {
            require(totalAmount > 0) { "총액은 0보다 커야 합니다: $totalAmount" }
            return Account(
                description = description,
                totalAmount = totalAmount,
                currentAmount = totalAmount,
                cardinal = cardinal,
            )
        }
    }
}
