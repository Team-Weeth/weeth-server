package com.weeth.domain.club.fixture

import com.weeth.domain.club.domain.entity.Club
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.fixture.UserTestFixture

object ClubMemberTestFixture {
    fun createActiveMember(
        club: Club = ClubTestFixture.createClub(),
        user: User = UserTestFixture.createActiveUser1(),
        memberRole: MemberRole = MemberRole.USER,
    ): ClubMember =
        ClubMember(
            club = club,
            user = user,
            memberStatus = MemberStatus.ACTIVE,
            memberRole = memberRole,
        )

    fun createWaitingMember(
        club: Club = ClubTestFixture.createClub(),
        user: User = UserTestFixture.createWaitingUser1(),
    ): ClubMember =
        ClubMember(
            club = club,
            user = user,
            memberStatus = MemberStatus.WAITING,
            memberRole = MemberRole.USER,
        )

    fun createAdminMember(
        club: Club = ClubTestFixture.createClub(),
        user: User = UserTestFixture.createAdmin(),
    ): ClubMember =
        ClubMember(
            club = club,
            user = user,
            memberStatus = MemberStatus.ACTIVE,
            memberRole = MemberRole.ADMIN,
        )
}
