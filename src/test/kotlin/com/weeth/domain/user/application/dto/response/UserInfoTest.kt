package com.weeth.domain.user.application.dto.response

import com.weeth.domain.club.domain.entity.Club
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.enums.PrimaryContact
import com.weeth.domain.club.domain.vo.ClubContact
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class UserInfoTest :
    StringSpec({
        val club =
            Club.create(
                name = "리츠",
                code = "LEETS001",
                schoolName = "가천대학교",
                clubContact =
                    ClubContact.from(
                        email = "leets@test.com",
                        phoneNumber = "01000000000",
                        primaryContact = PrimaryContact.PHONE,
                    ),
            )
        val user = UserTestFixture.createActiveUser1()

        "ofClubMember — ACTIVE 멤버는 실제 이름과 프로필을 노출한다" {
            val member = ClubMember(club = club, user = user)
            member.accept()

            val info = UserInfo.ofClubMember(member, "https://cdn.test/avatar.png")

            info.name shouldBe user.name
            info.profileImageUrl shouldBe "https://cdn.test/avatar.png"
            info.role shouldBe MemberRole.USER
        }

        "ofClubMember — LEFT 멤버는 이름을 익명 라벨로, 프로필을 null로 치환한다" {
            val member = ClubMember(club = club, user = user)
            member.accept()
            member.leave(LocalDateTime.now())

            val info = UserInfo.ofClubMember(member, "https://cdn.test/avatar.png")

            info.name shouldBe UserInfo.ANONYMOUS_USER_NAME
            info.profileImageUrl shouldBe null
            info.role shouldBe MemberRole.USER
        }

        "ofClubMember — LEFT 멤버라도 user id는 보존한다" {
            val member = ClubMember(club = club, user = user)
            member.accept()
            member.leave(LocalDateTime.now())

            val info = UserInfo.ofClubMember(member, null)

            info.id shouldBe user.id
        }
    })
