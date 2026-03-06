package com.weeth.domain.club.application.usecase.command

import com.weeth.domain.club.application.dto.request.ClubJoinRequest
import com.weeth.domain.club.application.exception.AlreadyJoinedException
import com.weeth.domain.club.application.exception.ClubCantJoinException
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
import io.mockk.mockk
import io.mockk.verify

class JoinClubUseCaseTest :
    DescribeSpec({
        val clubRepository = mockk<ClubRepository>()
        val clubMemberRepository = mockk<ClubMemberRepository>()
        val userReader = mockk<UserReader>()
        val clubMemberPolicy = mockk<ClubMemberPolicy>()

        val useCase =
            JoinClubUseCase(
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
            context("이미 다른 동아리에서 ACTIVE 상태로 활동 중인 경우") {
                it("MVP 단일 동아리 정책에 따라 가입할 수 없다") {
                    val targetClub = ClubTestFixture.createClub(code = "JOIN-CODE")
                    val anotherClub = ClubTestFixture.createClub()
                    val user = UserTestFixture.createActiveUser1()
                    val anotherClubMember =
                        ClubTestFixture.createClubMember(
                            club = anotherClub,
                            user = user,
                        )

                    every { clubRepository.getClubById(1L) } returns targetClub
                    every { userReader.getById(10L) } returns user
                    every { clubMemberRepository.findByClubIdAndUserId(1L, 10L) } returns null
                    every { clubMemberRepository.findAllByUserId(10L) } returns listOf(anotherClubMember)

                    shouldThrow<ClubCantJoinException> {
                        useCase.join(
                            clubId = 1L,
                            userId = 10L,
                            request = ClubJoinRequest(code = "JOIN-CODE"),
                        )
                    }

                    verify(exactly = 0) { clubMemberRepository.save(any()) }
                }
            }
        }
    })
