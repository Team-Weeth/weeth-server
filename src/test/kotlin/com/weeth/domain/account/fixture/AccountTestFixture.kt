package com.weeth.domain.account.fixture

import com.weeth.domain.account.domain.entity.Account
import com.weeth.domain.account.domain.enums.AccountStatus
import com.weeth.domain.club.domain.entity.Club
import com.weeth.domain.club.fixture.ClubTestFixture

object AccountTestFixture {
    fun createAccount(
        id: Long = 1L,
        club: Club = ClubTestFixture.createClub(),
        description: String = "2024년 2학기 회비",
        currentBalance: Int = 100_000,
        cardinal: Int = 40,
        status: AccountStatus = AccountStatus.ACTIVE,
    ): Account =
        Account(
            club = club,
            id = id,
            description = description,
            currentBalance = currentBalance,
            cardinal = cardinal,
            status = status,
        )
}
