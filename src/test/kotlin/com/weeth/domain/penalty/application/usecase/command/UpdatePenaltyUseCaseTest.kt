package com.weeth.domain.penalty.application.usecase.command

import com.weeth.domain.cardinal.fixture.CardinalTestFixture
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.penalty.application.dto.request.UpdatePenaltyRequest
import com.weeth.domain.penalty.application.exception.PenaltyNotFoundException
import com.weeth.domain.penalty.domain.enums.PenaltyType
import com.weeth.domain.penalty.domain.repository.PenaltyRepository
import com.weeth.domain.penalty.fixture.PenaltyTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class UpdatePenaltyUseCaseTest :
    DescribeSpec({
        val penaltyRepository = mockk<PenaltyRepository>()
        val clubMemberRepository = mockk<ClubMemberRepository>()
        val clubPermissionPolicy = mockk<ClubPermissionPolicy>()
        val useCase =
            UpdatePenaltyUseCase(
                penaltyRepository = penaltyRepository,
                clubMemberRepository = clubMemberRepository,
                clubPermissionPolicy = clubPermissionPolicy,
            )

        beforeTest {
            clearMocks(penaltyRepository, clubMemberRepository, clubPermissionPolicy)
        }

        describe("update") {
            it("관리자 권한이 없으면 예외를 던진다") {
                every { clubPermissionPolicy.requireAdmin(any(), any()) } throws RuntimeException()

                shouldThrow<RuntimeException> {
                    useCase.update(
                        clubId = 1L,
                        userId = 1L,
                        request =
                            UpdatePenaltyRequest(
                                penaltyId = 1L,
                                penaltyDescription = "수정됨",
                                score = null,
                            ),
                    )
                }

                verify {
                    clubPermissionPolicy.requireAdmin(1L, 1L)
                }
            }

            it("페널티가 없으면 예외를 던진다") {
                val clubMember = ClubMemberTestFixture.createActiveMember()
                every { clubPermissionPolicy.requireAdmin(any(), any()) } returns clubMember
                every { penaltyRepository.findByIdWithLock(any()) } returns null

                shouldThrow<PenaltyNotFoundException> {
                    useCase.update(
                        clubId = clubMember.club.id,
                        userId = 1L,
                        request =
                            UpdatePenaltyRequest(
                                penaltyId = 1L,
                                penaltyDescription = "수정됨",
                                score = null,
                            ),
                    )
                }
            }

            it("다른 클럽의 페널티이면 예외를 던진다") {
                val clubMember = ClubMemberTestFixture.createActiveMember()
                val penalty = PenaltyTestFixture.createPenalty()
                every { clubPermissionPolicy.requireAdmin(any(), any()) } returns clubMember
                every { penaltyRepository.findByIdWithLock(1L) } returns penalty

                shouldThrow<PenaltyNotFoundException> {
                    useCase.update(
                        clubId = 999L,
                        userId = 1L,
                        request =
                            UpdatePenaltyRequest(
                                penaltyId = 1L,
                                penaltyDescription = "수정됨",
                                score = null,
                            ),
                    )
                }
            }

            it("페널티 설명만 수정한다") {
                val penalty = PenaltyTestFixture.createPenalty(penaltyDescription = "원래 사유")
                every { clubPermissionPolicy.requireAdmin(any(), any()) } returns penalty.clubMember
                every { penaltyRepository.findByIdWithLock(1L) } returns penalty

                useCase.update(
                    clubId = penalty.clubMember.club.id,
                    userId = 1L,
                    request =
                        UpdatePenaltyRequest(
                            penaltyId = 1L,
                            penaltyDescription = "수정된 사유",
                            score = null,
                        ),
                )

                penalty.penaltyDescription shouldBe "수정된 사유"
                penalty.score shouldBe 1
            }

            it("페널티 점수만 수정한다") {
                val penalty =
                    PenaltyTestFixture.createPenalty(
                        penaltyType = PenaltyType.PENALTY,
                        score = 2,
                    )
                val clubMember = penalty.clubMember
                every { clubPermissionPolicy.requireAdmin(any(), any()) } returns clubMember
                every { penaltyRepository.findByIdWithLock(any()) } returns penalty
                every { clubMemberRepository.findByIdWithLock(any()) } returns clubMember

                useCase.update(
                    clubId = penalty.clubMember.club.id,
                    userId = 1L,
                    request =
                        UpdatePenaltyRequest(
                            penaltyId = 1L,
                            penaltyDescription = null,
                            score = 4,
                        ),
                )

                penalty.score shouldBe 4
                verify {
                    clubMemberRepository.findByIdWithLock(clubMember.id)
                }
            }

            it("페널티 점수 증가 시 clubMember의 penaltyCount를 증가시킨다") {
                val penalty =
                    PenaltyTestFixture.createPenalty(
                        penaltyType = PenaltyType.PENALTY,
                        score = 2,
                    )
                val clubMember = penalty.clubMember
                every { clubPermissionPolicy.requireAdmin(any(), any()) } returns clubMember
                every { penaltyRepository.findByIdWithLock(any()) } returns penalty
                every { clubMemberRepository.findByIdWithLock(any()) } returns clubMember

                useCase.update(
                    clubId = penalty.clubMember.club.id,
                    userId = 1L,
                    request =
                        UpdatePenaltyRequest(
                            penaltyId = 1L,
                            penaltyDescription = null,
                            score = 5,
                        ),
                )

                penalty.score shouldBe 5
            }

            it("경고 점수 변경 시 clubMember의 warningCount를 조정한다") {
                val penalty =
                    PenaltyTestFixture.createWarning(
                        penaltyDescription = "경고",
                        score = 1,
                    )
                val clubMember = penalty.clubMember
                every { clubPermissionPolicy.requireAdmin(any(), any()) } returns clubMember
                every { penaltyRepository.findByIdWithLock(any()) } returns penalty
                every { clubMemberRepository.findByIdWithLock(any()) } returns clubMember

                useCase.update(
                    clubId = penalty.clubMember.club.id,
                    userId = 1L,
                    request =
                        UpdatePenaltyRequest(
                            penaltyId = 1L,
                            penaltyDescription = null,
                            score = 3,
                        ),
                )

                penalty.score shouldBe 3
            }

            it("점수가 변경되지 않으면 clubMember를 조회하지 않는다") {
                val penalty = PenaltyTestFixture.createPenalty(score = 2)
                every { clubPermissionPolicy.requireAdmin(any(), any()) } returns penalty.clubMember
                every { penaltyRepository.findByIdWithLock(any()) } returns penalty

                useCase.update(
                    clubId = penalty.clubMember.club.id,
                    userId = 1L,
                    request =
                        UpdatePenaltyRequest(
                            penaltyId = 1L,
                            penaltyDescription = "수정된 사유",
                            score = 2,
                        ),
                )

                verify(exactly = 0) {
                    clubMemberRepository.findByIdWithLock(any())
                }
            }

            it("페널티 설명과 점수를 모두 수정한다") {
                val penalty =
                    PenaltyTestFixture.createPenalty(
                        penaltyDescription = "원래 사유",
                        score = 1,
                    )
                val clubMember = penalty.clubMember
                every { clubPermissionPolicy.requireAdmin(any(), any()) } returns clubMember
                every { penaltyRepository.findByIdWithLock(any()) } returns penalty
                every { clubMemberRepository.findByIdWithLock(any()) } returns clubMember

                useCase.update(
                    clubId = penalty.clubMember.club.id,
                    userId = 1L,
                    request =
                        UpdatePenaltyRequest(
                            penaltyId = 1L,
                            penaltyDescription = "수정된 사유",
                            score = 3,
                        ),
                )

                penalty.penaltyDescription shouldBe "수정된 사유"
                penalty.score shouldBe 3
            }

            it("공백만 있는 페널티 설명은 무시한다") {
                val penalty = PenaltyTestFixture.createPenalty(penaltyDescription = "원래 사유")
                every { clubPermissionPolicy.requireAdmin(any(), any()) } returns penalty.clubMember
                every { penaltyRepository.findByIdWithLock(any()) } returns penalty

                useCase.update(
                    clubId = penalty.clubMember.club.id,
                    userId = 1L,
                    request =
                        UpdatePenaltyRequest(
                            penaltyId = 1L,
                            penaltyDescription = "   ",
                            score = null,
                        ),
                )

                penalty.penaltyDescription shouldBe "원래 사유"
            }
        }
    })
