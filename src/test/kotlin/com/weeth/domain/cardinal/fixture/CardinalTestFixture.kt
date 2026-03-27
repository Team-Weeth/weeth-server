package com.weeth.domain.cardinal.fixture

import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.cardinal.domain.enums.CardinalStatus
import com.weeth.domain.club.domain.entity.Club
import com.weeth.domain.club.fixture.ClubTestFixture

object CardinalTestFixture {
    fun createCardinal(
        id: Long? = null,
        club: Club = ClubTestFixture.createClub(),
        cardinalNumber: Int,
    ): Cardinal =
        Cardinal(
            club = club,
            id = id ?: 0L,
            cardinalNumber = cardinalNumber,
            status = CardinalStatus.DONE,
        )

    fun createCardinalInProgress(
        id: Long? = null,
        club: Club = ClubTestFixture.createClub(),
        cardinalNumber: Int,
    ): Cardinal =
        Cardinal(
            club = club,
            id = id ?: 0L,
            cardinalNumber = cardinalNumber,
            status = CardinalStatus.IN_PROGRESS,
        )
}
