package com.weeth.domain.club.fixture

import com.weeth.domain.club.domain.entity.Club
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.fixture.UserTestFixture
import org.springframework.test.util.ReflectionTestUtils

object ClubMemberTestFixture {
    fun createActiveMember(
        id: Long = 0L,
        club: Club = ClubTestFixture.createClub(),
        user: User = UserTestFixture.createActiveUser1(),
        memberRole: MemberRole = MemberRole.USER,
    ): ClubMember =
        ClubMember(
            club = club,
            user = user,
            memberStatus = MemberStatus.ACTIVE,
            memberRole = memberRole,
        ).also { if (id != 0L) ReflectionTestUtils.setField(it, "id", id) }

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

    fun createBannedMember(
        id: Long = 0L,
        club: Club = ClubTestFixture.createClub(),
        user: User = UserTestFixture.createActiveUser1(),
        memberRole: MemberRole = MemberRole.USER,
    ): ClubMember =
        ClubMember(
            club = club,
            user = user,
            memberStatus = MemberStatus.BANNED,
            memberRole = memberRole,
        ).also { if (id != 0L) ReflectionTestUtils.setField(it, "id", id) }

    fun createLeadMember(
        club: Club = ClubTestFixture.createClub(),
        user: User = UserTestFixture.createActiveUser1(),
    ): ClubMember =
        ClubMember(
            club = club,
            user = user,
            memberStatus = MemberStatus.ACTIVE,
            memberRole = MemberRole.LEAD,
        )
}
