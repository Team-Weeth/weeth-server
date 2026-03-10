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
        year: Int,
        semester: Int,
    ): Cardinal =
        Cardinal(
            club = club,
            id = id ?: 0L,
            cardinalNumber = cardinalNumber,
            year = year,
            semester = semester,
            status = CardinalStatus.DONE,
        )

    fun createCardinalInProgress(
        id: Long? = null,
        club: Club = ClubTestFixture.createClub(),
        cardinalNumber: Int,
        year: Int,
        semester: Int,
    ): Cardinal =
        Cardinal(
            club = club,
            id = id ?: 0L,
            cardinalNumber = cardinalNumber,
            year = year,
            semester = semester,
            status = CardinalStatus.IN_PROGRESS,
        )
}
