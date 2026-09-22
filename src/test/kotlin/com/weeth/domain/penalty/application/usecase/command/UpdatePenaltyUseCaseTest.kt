package com.weeth.domain.penalty.application.usecase.command

import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.penalty.application.dto.request.UpdatePenaltyRequest
import com.weeth.domain.penalty.application.exception.PenaltyNotFoundException
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
        val clubPermissionPolicy = mockk<ClubPermissionPolicy>()
        val useCase =
            UpdatePenaltyUseCase(
                penaltyRepository = penaltyRepository,
                clubPermissionPolicy = clubPermissionPolicy,
            )

        beforeTest {
            clearMocks(penaltyRepository, clubPermissionPolicy)
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
                            ),
                    )
                }
            }

            it("페널티 설명을 수정한다") {
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
                        ),
                )

                penalty.penaltyDescription shouldBe "수정된 사유"
            }

            it("설명이 null이면 변경하지 않는다") {
                val penalty = PenaltyTestFixture.createPenalty(penaltyDescription = "원래 사유")
                every { clubPermissionPolicy.requireAdmin(any(), any()) } returns penalty.clubMember
                every { penaltyRepository.findByIdWithLock(any()) } returns penalty

                useCase.update(
                    clubId = penalty.clubMember.club.id,
                    userId = 1L,
                    request =
                        UpdatePenaltyRequest(
                            penaltyId = 1L,
                            penaltyDescription = null,
                        ),
                )

                penalty.penaltyDescription shouldBe "원래 사유"
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
                        ),
                )

                penalty.penaltyDescription shouldBe "원래 사유"
            }
        }
    })
