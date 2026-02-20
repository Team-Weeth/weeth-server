package com.weeth.domain.account.domain.entity

import com.weeth.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.time.LocalDate

@Entity
class Receipt(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receipt_id")
    val id: Long = 0,
    @Column
    var description: String?,
    @Column
    var source: String?,
    @Column(nullable = false)
    var amount: Int,
    @Column(nullable = false)
    var date: LocalDate,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    val account: Account,
) : BaseEntity() {
    fun update(
        description: String?,
        source: String?,
        amount: Int,
        date: LocalDate,
    ) {
        require(amount > 0) { "금액은 0보다 커야 합니다: $amount" }
        this.description = description
        this.source = source
        this.amount = amount
        this.date = date
    }

    companion object {
        fun create(
            description: String?,
            source: String?,
            amount: Int,
            date: LocalDate,
            account: Account,
        ): Receipt {
            require(amount > 0) { "금액은 0보다 커야 합니다: $amount" }
            return Receipt(
                description = description,
                source = source,
                amount = amount,
                date = date,
                account = account,
            )
        }
    }
}
