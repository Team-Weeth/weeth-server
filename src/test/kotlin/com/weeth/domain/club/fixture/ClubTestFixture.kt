package com.weeth.domain.club.fixture

import com.weeth.domain.club.domain.entity.Club
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.enums.PrimaryContact
import com.weeth.domain.club.domain.vo.ClubContact
import com.weeth.domain.user.fixture.UserTestFixture
import org.springframework.test.util.ReflectionTestUtils

object ClubTestFixture {
    fun createClub(
        name: String = "테스트 동아리",
        code: String = "TEST001",
        description: String? = "테스트 동아리 소개",
        schoolName: String = "가천대학교",
        clubContact: ClubContact =
            ClubContact.from(
                email = "test@leets.com",
                phoneNumber = "010-0000-0000",
                primaryContact = PrimaryContact.PHONE,
            ),
    ): Club {
        val club =
            Club.create(
                name = name,
                code = code,
                description = description,
                schoolName = schoolName,
                clubContact = clubContact,
            )
        return club
    }

    fun createClubMember(
        club: Club = createClub(),
        user: com.weeth.domain.user.domain.entity.User = UserTestFixture.createActiveUser1(),
        memberStatus: MemberStatus = MemberStatus.ACTIVE,
        memberRole: MemberRole = MemberRole.USER,
    ): ClubMember {
        val member =
            ClubMember(
                club = club,
                user = user,
                memberStatus = memberStatus,
                memberRole = memberRole,
            )
        return member
    }
}
