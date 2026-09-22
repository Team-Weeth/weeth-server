package com.weeth.domain.penalty.application.usecase.command

import com.weeth.domain.cardinal.fixture.CardinalTestFixture
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.repository.ClubReader
import com.weeth.domain.club.domain.service.ClubMemberCardinalPolicy
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.penalty.application.dto.request.SavePenaltyRequest
import com.weeth.domain.penalty.application.exception.WarningNotEnabledException
import com.weeth.domain.penalty.application.mapper.PenaltyMapper
import com.weeth.domain.penalty.domain.entity.Penalty
import com.weeth.domain.penalty.domain.enums.PenaltyType
import com.weeth.domain.penalty.domain.repository.PenaltyRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.test.util.ReflectionTestUtils

class SavePenaltyUseCaseTest :
    DescribeSpec({
        val penaltyRepository = mockk<PenaltyRepository>()
        val clubMemberRepository = mockk<ClubMemberRepository>()
        val clubMemberPolicy = mockk<ClubMemberPolicy>()
        val clubPermissionPolicy = mockk<ClubPermissionPolicy>()
        val clubMemberCardinalPolicy = mockk<ClubMemberCardinalPolicy>()
        val clubReader = mockk<ClubReader>()
        val mapper = PenaltyMapper(mockk<FileAccessUrlPort>())
        val useCase =
            SavePenaltyUseCase(
                penaltyRepository = penaltyRepository,
                clubMemberRepository = clubMemberRepository,
                clubMemberPolicy = clubMemberPolicy,
                clubPermissionPolicy = clubPermissionPolicy,
                clubMemberCardinalPolicy = clubMemberCardinalPolicy,
                clubReader = clubReader,
                mapper = mapper,
            )

        beforeTest {
            clearMocks(
                penaltyRepository,
                clubMemberRepository,
                clubMemberPolicy,
                clubPermissionPolicy,
                clubMemberCardinalPolicy,
                clubReader,
            )
            every { clubPermissionPolicy.requireAdmin(any(), any()) } returns ClubMemberTestFixture.createAdminMember()
            every { penaltyRepository.save(any()) } answers { firstArg() }
        }

        fun setupMember(warningEnabled: Boolean = true): ClubMember {
            val club = ClubTestFixture.createClub()
            ReflectionTestUtils.setField(club, "warningEnabled", warningEnabled)
            val clubMember = ClubMemberTestFixture.createActiveMember(club = club)
            val cardinal = CardinalTestFixture.createCardinal(club = club, cardinalNumber = 1)

            every { clubReader.getClubById(any()) } returns club
            every { clubMemberPolicy.getActiveMember(any(), any()) } returns clubMember
            every { clubMemberCardinalPolicy.getCurrentCardinal(clubMember) } returns cardinal
            every { clubMemberRepository.findByIdWithLock(clubMember.id) } returns clubMember
            return clubMember
        }

        describe("save") {
            it("WARNING인데 club의 경고 기능이 꺼져있으면 예외를 던진다") {
                setupMember(warningEnabled = false)

                shouldThrow<WarningNotEnabledException> {
                    useCase.save(
                        clubId = 1L,
                        userId = 1L,
                        request =
                            SavePenaltyRequest(
                                userIds = listOf(1L),
                                penaltyDescription = "경고 테스트",
                                penaltyType = PenaltyType.WARNING,
                            ),
                    )
                }
            }

            it("PENALTY 타입은 penaltyCount만 증가하고 추가 자동 생성이 없다") {
                val clubMember = setupMember()

                useCase.save(
                    clubId = clubMember.club.id,
                    userId = 1L,
                    request =
                        SavePenaltyRequest(
                            userIds = listOf(clubMember.user.id),
                            penaltyDescription = "정기모임 무단 불참",
                            penaltyType = PenaltyType.PENALTY,
                        ),
                )

                clubMember.penaltyCount shouldBe 1
                clubMember.warningCount shouldBe 0
                verify(exactly = 1) { penaltyRepository.save(any()) }
            }

            it("WARNING이 threshold 미만이면 경고 1건만 저장되고 자동 전환 페널티는 생성되지 않는다") {
                val clubMember = setupMember()

                useCase.save(
                    clubId = clubMember.club.id,
                    userId = 1L,
                    request =
                        SavePenaltyRequest(
                            userIds = listOf(clubMember.user.id),
                            penaltyDescription = "지각",
                            penaltyType = PenaltyType.WARNING,
                        ),
                )

                clubMember.warningCount shouldBe 1
                clubMember.penaltyCount shouldBe 0
                verify(exactly = 1) { penaltyRepository.save(any()) }
            }

            it("WARNING이 threshold에 도달하면 자동 전환된 Penalty가 추가로 저장된다") {
                val clubMember = setupMember()
                val threshold = ClubMember.WARNING_TO_PENALTY_THRESHOLD
                ReflectionTestUtils.setField(clubMember, "warningCount", threshold - 1)

                val savedSlot = mutableListOf<Penalty>()
                every { penaltyRepository.save(capture(savedSlot)) } answers { firstArg() }

                useCase.save(
                    clubId = clubMember.club.id,
                    userId = 1L,
                    request =
                        SavePenaltyRequest(
                            userIds = listOf(clubMember.user.id),
                            penaltyDescription = "지각",
                            penaltyType = PenaltyType.WARNING,
                        ),
                )

                clubMember.warningCount shouldBe 0
                clubMember.penaltyCount shouldBe 1
                verify(exactly = 2) { penaltyRepository.save(any()) }

                val autoConverted = savedSlot.last()
                autoConverted.penaltyType shouldBe PenaltyType.PENALTY
                autoConverted.penaltyDescription shouldBe "누적경고 ${threshold}회"
            }
        }
    })
