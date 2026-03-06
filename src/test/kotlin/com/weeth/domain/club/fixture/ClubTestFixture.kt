package com.weeth.domain.club.fixture

import com.weeth.domain.club.domain.entity.Club
import com.weeth.domain.club.domain.vo.ClubContact

object ClubTestFixture {
    fun createClub(
        name: String = "테스트 동아리",
        code: String = "TEST001",
        description: String? = "테스트 동아리 소개",
        schoolName: String = "가천대학교",
        clubContact: ClubContact = ClubContact.from(email = "test@leets.com", phoneNumber = null),
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
}
