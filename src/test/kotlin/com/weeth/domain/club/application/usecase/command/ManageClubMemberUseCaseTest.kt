package com.weeth.domain.club.application.usecase.command

import com.weeth.domain.club.application.dto.request.ClubJoinRequest
import com.weeth.domain.club.application.exception.ClubJoinLimitExceededException
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.repository.ClubRepository
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class ManageClubMemberUseCaseTest :
    DescribeSpec({
        val clubRepository = mockk<ClubRepository>()
        val clubMemberRepository = mockk<ClubMemberRepository>()
        val userReader = mockk<UserReader>()
        val clubMemberPolicy = mockk<ClubMemberPolicy>()

        val useCase =
            ManageClubMemberUsecase(
                clubRepository = clubRepository,
                clubMemberRepository = clubMemberRepository,
                userReader = userReader,
                clubMemberPolicy = clubMemberPolicy,
            )

        beforeTest {
            clearMocks(clubRepository, clubMemberRepository, userReader, clubMemberPolicy)
            every { clubMemberRepository.save(any()) } answers { firstArg() }
        }

        describe("join") {
            context("이미 USER로 1개 동아리에 가입한 사용자가 가입 시도하는 경우") {
                it("ClubJoinLimitExceededException이 발생한다") {
                    val targetClub = ClubTestFixture.createClub(code = "JOIN-CODE")
                    val user = UserTestFixture.createActiveUser1()

                    every { clubRepository.getClubById(1L) } returns targetClub
                    every { userReader.getByIdWithLock(10L) } returns user
                    every { clubMemberRepository.findByClubIdAndUserId(1L, 10L) } returns null
                    every { clubMemberPolicy.validateJoinLimit(10L) } throws ClubJoinLimitExceededException()

                    shouldThrow<ClubJoinLimitExceededException> {
                        useCase.join(
                            clubId = 1L,
                            userId = 10L,
                            request = ClubJoinRequest(code = "JOIN-CODE"),
                        )
                    }

                    verify(exactly = 0) { clubMemberRepository.save(any()) }
                }
            }

            context("LEAD로 1개 동아리를 생성한 사용자가 USER로 가입 시도하는 경우") {
                it("역할이 다르므로 가입에 성공한다") {
                    val targetClub = ClubTestFixture.createClub(code = "JOIN-CODE")
                    val user = UserTestFixture.createActiveUser1()

                    every { clubRepository.getClubById(1L) } returns targetClub
                    every { userReader.getByIdWithLock(10L) } returns user
                    every { clubMemberRepository.findByClubIdAndUserId(1L, 10L) } returns null
                    justRun { clubMemberPolicy.validateJoinLimit(10L) }

                    useCase.join(
                        clubId = 1L,
                        userId = 10L,
                        request = ClubJoinRequest(code = "JOIN-CODE"),
                    )

                    verify(exactly = 1) { clubMemberRepository.save(any()) }
                }
            }
        }
    })
