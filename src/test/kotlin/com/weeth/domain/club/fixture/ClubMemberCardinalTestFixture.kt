package com.weeth.domain.club.fixture

import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.cardinal.fixture.CardinalTestFixture
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.entity.ClubMemberCardinal

object ClubMemberCardinalTestFixture {
    fun create(
        clubMember: ClubMember = ClubMemberTestFixture.createActiveMember(),
        cardinal: Cardinal = CardinalTestFixture.createCardinal(cardinalNumber = 1),
    ): ClubMemberCardinal =
        ClubMemberCardinal(
            clubMember = clubMember,
            cardinal = cardinal,
        )
}
