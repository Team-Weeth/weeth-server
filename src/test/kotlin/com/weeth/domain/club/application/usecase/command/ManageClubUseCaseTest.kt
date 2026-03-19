package com.weeth.domain.club.application.usecase.command

import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.cardinal.domain.enums.CardinalStatus
import com.weeth.domain.cardinal.domain.repository.CardinalRepository
import com.weeth.domain.club.application.dto.request.ClubCreateRequest
import com.weeth.domain.club.application.dto.request.ClubUpdateRequest
import com.weeth.domain.club.domain.entity.ClubMemberCardinal
import com.weeth.domain.club.domain.repository.ClubMemberCardinalRepository
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.repository.ClubRepository
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.domain.vo.ClubContact
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class ManageClubUseCaseTest :
    DescribeSpec({
        val clubRepository = mockk<ClubRepository>()
        val clubMemberRepository = mockk<ClubMemberRepository>()
        val cardinalRepository = mockk<CardinalRepository>()
        val clubMemberCardinalRepository = mockk<ClubMemberCardinalRepository>()
        val userReader = mockk<UserReader>()
        val clubMemberPolicy = mockk<ClubMemberPolicy>()
        val useCase =
            ManageClubUseCase(
                clubRepository,
                clubMemberRepository,
                cardinalRepository,
                clubMemberCardinalRepository,
                userReader,
                clubMemberPolicy,
            )
        val adminMember =
            com.weeth.domain.club.fixture.ClubMemberTestFixture
                .createAdminMember()

        beforeTest {
            clearMocks(
                clubRepository,
                clubMemberRepository,
                cardinalRepository,
                clubMemberCardinalRepository,
                userReader,
                clubMemberPolicy,
            )
            every { clubRepository.save(any()) } answers { firstArg() }
            every { clubMemberRepository.save(any()) } answers { firstArg() }
            every { cardinalRepository.saveAll(any<List<Cardinal>>()) } answers { firstArg() }
            every { clubMemberCardinalRepository.save(any()) } answers { firstArg() }
        }

        describe("create") {
            val user = UserTestFixture.createActiveUser1()

            context("N기 동아리를 개설하는 경우") {
                it("1기부터 N기까지 Cardinal이 생성되며, 마지막 기수만 IN_PROGRESS이다") {
                    val cardinalSlot = slot<List<Cardinal>>()
                    every { userReader.getById(10L) } returns user
                    every { cardinalRepository.saveAll(capture(cardinalSlot)) } answers { firstArg() }

                    useCase.create(
                        10L,
                        ClubCreateRequest(
                            name = "테스트",
                            schoolName = "가천대",
                            currentCardinal = 3,
                            contactEmail = "test@example.com",
                        ),
                    )

                    val cardinals = cardinalSlot.captured
                    cardinals.size shouldBe 3
                    cardinals[0].cardinalNumber shouldBe 1
                    cardinals[0].status shouldBe CardinalStatus.DONE
                    cardinals[1].cardinalNumber shouldBe 2
                    cardinals[1].status shouldBe CardinalStatus.DONE
                    cardinals[2].cardinalNumber shouldBe 3
                    cardinals[2].status shouldBe CardinalStatus.IN_PROGRESS
                }

                it("LEAD 멤버가 최신 기수에 ClubMemberCardinal로 배정된다") {
                    every { userReader.getById(10L) } returns user

                    useCase.create(
                        10L,
                        ClubCreateRequest(
                            name = "테스트",
                            schoolName = "가천대",
                            currentCardinal = 3,
                            contactEmail = "test@example.com",
                        ),
                    )

                    verify(exactly = 1) { clubMemberCardinalRepository.save(any<ClubMemberCardinal>()) }
                }

                it("1기만 있는 동아리 개설 시 Cardinal 1개가 IN_PROGRESS로 생성된다") {
                    val cardinalSlot = slot<List<Cardinal>>()
                    every { userReader.getById(10L) } returns user
                    every { cardinalRepository.saveAll(capture(cardinalSlot)) } answers { firstArg() }

                    useCase.create(
                        10L,
                        ClubCreateRequest(
                            name = "테스트",
                            schoolName = "가천대",
                            currentCardinal = 1,
                            contactEmail = "test@example.com",
                        ),
                    )

                    val cardinals = cardinalSlot.captured
                    cardinals.size shouldBe 1
                    cardinals[0].cardinalNumber shouldBe 1
                    cardinals[0].status shouldBe CardinalStatus.IN_PROGRESS
                }
            }
        }

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
