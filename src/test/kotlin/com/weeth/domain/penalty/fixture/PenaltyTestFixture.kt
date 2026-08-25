package com.weeth.domain.penalty.fixture

import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.cardinal.fixture.CardinalTestFixture
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.penalty.domain.entity.Penalty
import com.weeth.domain.penalty.domain.enums.PenaltyType

object PenaltyTestFixture {
    fun createPenalty(
        clubMember: ClubMember = ClubMemberTestFixture.createActiveMember(),
        cardinal: Cardinal = CardinalTestFixture.createCardinal(club = clubMember.club, cardinalNumber = 1),
        penaltyDescription: String = "정기모임 무단 불참",
        penaltyType: PenaltyType = PenaltyType.PENALTY,
        score: Int = 1,
    ) = Penalty(
        clubMember = clubMember,
        cardinal = cardinal,
        penaltyDescription = penaltyDescription,
        penaltyType = penaltyType,
        score = score,
    )

    fun createWarning(
        clubMember: ClubMember = ClubMemberTestFixture.createActiveMember(),
        cardinal: Cardinal = CardinalTestFixture.createCardinal(club = clubMember.club, cardinalNumber = 1),
        penaltyDescription: String = "경고",
        score: Int = 1,
    ) = Penalty(
        clubMember = clubMember,
        cardinal = cardinal,
        penaltyDescription = penaltyDescription,
        penaltyType = PenaltyType.WARNING,
        score = score,
    )
}
