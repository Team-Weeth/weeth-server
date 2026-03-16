package com.weeth.domain.club.application.usecase.command

import com.weeth.domain.club.application.dto.request.ClubUpdateRequest
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.repository.ClubRepository
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.domain.vo.ClubContact
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.user.domain.repository.UserReader
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class ManageClubUseCaseTest :
    DescribeSpec({
        val clubRepository = mockk<ClubRepository>()
        val clubMemberRepository = mockk<ClubMemberRepository>()
        val userReader = mockk<UserReader>()
        val clubMemberPolicy = mockk<ClubMemberPolicy>()
        val useCase = ManageClubUseCase(clubRepository, clubMemberRepository, userReader, clubMemberPolicy)
        val adminMember =
            com.weeth.domain.club.fixture.ClubMemberTestFixture
                .createAdminMember()

        describe("update") {
            it("null 필드는 유지하고 전달된 필드만 수정한다") {
                val club =
                    ClubTestFixture.createClub(
                        name = "기존 동아리",
                        schoolName = "가천대학교",
                        description = "기존 소개",
                        clubContact = ClubContact.from(email = "club@example.com", phoneNumber = "010-1111-2222"),
                    )
                club.update(
                    null,
                    null,
                    null,
                    null,
                    null,
                    "https://example.com/profile.png",
                    "https://example.com/background.png",
                )

                every { clubMemberPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubRepository.getClubById(1L) } returns club

                useCase.update(
                    1L,
                    10L,
                    ClubUpdateRequest(
                        schoolName = "연세대학교",
                        contactPhoneNumber = "010-9999-8888",
                    ),
                )

                club.name shouldBe "기존 동아리"
                club.schoolName shouldBe "연세대학교"
                club.description shouldBe "기존 소개"
                club.clubContact.email shouldBe "club@example.com"
                club.clubContact.phoneNumber shouldBe "010-9999-8888"
                club.profileImageUrl shouldBe "https://example.com/profile.png"
                club.backgroundImageUrl shouldBe "https://example.com/background.png"
            }

            it("모든 필드가 null이면 기존 값이 유지된다") {
                val club =
                    ClubTestFixture.createClub(
                        description = "기존 소개",
                        clubContact = ClubContact.from(email = "club@example.com", phoneNumber = null),
                    )
                every { clubMemberPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubRepository.getClubById(1L) } returns club

                useCase.update(1L, 10L, ClubUpdateRequest())

                club.name shouldBe "테스트 동아리"
                club.schoolName shouldBe "가천대학교"
                club.description shouldBe "기존 소개"
                club.clubContact.email shouldBe "club@example.com"
                club.clubContact.phoneNumber shouldBe null
            }
        }

        describe("deleteProfileImage") {
            it("프로필 사진만 삭제하고 배경 사진은 유지한다") {
                val club =
                    ClubTestFixture.createClub(
                        clubContact = ClubContact.from(email = "club@example.com", phoneNumber = "010-1111-2222"),
                    )
                club.update(
                    null,
                    null,
                    null,
                    null,
                    null,
                    "https://example.com/profile.png",
                    "https://example.com/background.png",
                )

                every { clubMemberPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubRepository.getClubById(1L) } returns club

                useCase.deleteProfileImage(1L, 10L)

                club.profileImageUrl shouldBe null
                club.backgroundImageUrl shouldBe "https://example.com/background.png"
            }
        }

        describe("deleteBackgroundImage") {
            it("배경 사진만 삭제하고 프로필 사진은 유지한다") {
                val club =
                    ClubTestFixture.createClub(
                        clubContact = ClubContact.from(email = "club@example.com", phoneNumber = "010-1111-2222"),
                    )
                club.update(
                    null,
                    null,
                    null,
                    null,
                    null,
                    "https://example.com/profile.png",
                    "https://example.com/background.png",
                )

                every { clubMemberPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubRepository.getClubById(1L) } returns club

                useCase.deleteBackgroundImage(1L, 10L)

                club.profileImageUrl shouldBe "https://example.com/profile.png"
                club.backgroundImageUrl shouldBe null
            }
        }
    })
