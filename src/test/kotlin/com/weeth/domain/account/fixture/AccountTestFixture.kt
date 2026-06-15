package com.weeth.domain.account.fixture

import com.weeth.domain.account.domain.entity.Account
import com.weeth.domain.club.domain.entity.Club
import com.weeth.domain.club.fixture.ClubTestFixture

object AccountTestFixture {
    fun createAccount(
        id: Long = 1L,
        club: Club = ClubTestFixture.createClub(),
        description: String = "2024년 2학기 회비",
        totalAmount: Int = 100_000,
        currentAmount: Int = 100_000,
        currentBalance: Int = currentAmount,
        cardinal: Int = 40,
    ): Account =
        Account(
            club = club,
            id = id,
            description = description,
            totalAmount = totalAmount,
            currentAmount = currentAmount,
            currentBalance = currentBalance,
            cardinal = cardinal,
        )
}
