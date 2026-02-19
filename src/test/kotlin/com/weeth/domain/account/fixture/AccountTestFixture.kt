package com.weeth.domain.account.fixture

import com.weeth.domain.account.domain.entity.Account

object AccountTestFixture {
    fun createAccount(
        id: Long = 1L,
        description: String = "2024년 2학기 회비",
        totalAmount: Int = 100_000,
        currentAmount: Int = 100_000,
        cardinal: Int = 40,
    ): Account =
        Account
            .builder()
            .id(id)
            .description(description)
            .totalAmount(totalAmount)
            .currentAmount(currentAmount)
            .cardinal(cardinal)
            .receipts(mutableListOf())
            .build()
}
