package com.weeth.domain.club.application.usecase.command

import com.weeth.domain.attendance.domain.service.AttendanceInitializer
import com.weeth.domain.cardinal.application.exception.CardinalNotFoundException
import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.cardinal.fixture.CardinalTestFixture
import com.weeth.domain.club.application.dto.request.ClubJoinRequest
import com.weeth.domain.club.application.dto.request.ClubMemberCardinalSetRequest
import com.weeth.domain.club.application.exception.CannotLeaveAsLeadException
import com.weeth.domain.club.application.exception.CardinalAlreadySetException
import com.weeth.domain.club.application.exception.ClubJoinLimitExceededException
import com.weeth.domain.club.domain.entity.ClubMemberCardinal
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.repository.ClubMemberCardinalRepository
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.repository.ClubRepository
import com.weeth.domain.club.domain.service.ClubActivityDeletionPolicy
import com.weeth.domain.club.domain.service.ClubJoinPolicy
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.user.application.exception.UserInActiveException
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class ManageClubMemberUseCaseTest :
    DescribeSpec({
        val clubRepository = mockk<ClubRepository>()
        val clubMemberRepository = mockk<ClubMemberRepository>()
        val clubMemberCardinalRepository = mockk<ClubMemberCardinalRepository>(relaxed = true)
        val cardinalReader = mockk<CardinalReader>()
        val attendanceInitializer = mockk<AttendanceInitializer>(relaxed = true)
        val userReader = mockk<UserReader>()
        val clubMemberPolicy = mockk<ClubMemberPolicy>()
        val clubJoinPolicy = mockk<ClubJoinPolicy>()
        val clubActivityDeletionPolicy = mockk<ClubActivityDeletionPolicy>()
        val clock = Clock.fixed(Instant.parse("2026-06-08T03:00:00Z"), ZoneId.of("Asia/Seoul"))

        val useCase =
            ManageClubMemberUsecase(
                clubRepository = clubRepository,
                clubMemberRepository = clubMemberRepository,
                clubMemberCardinalRepository = clubMemberCardinalRepository,
                cardinalReader = cardinalReader,
                attendanceInitializer = attendanceInitializer,
                userReader = userReader,
                clubMemberPolicy = clubMemberPolicy,
                clubJoinPolicy = clubJoinPolicy,
                clubActivityDeletionPolicy = clubActivityDeletionPolicy,
                clock = clock,
            )

        beforeTest {
            clearMocks(
                clubRepository,
                clubMemberRepository,
                clubMemberCardinalRepository,
                cardinalReader,
                attendanceInitializer,
                userReader,
                clubMemberPolicy,
                clubJoinPolicy,
                clubActivityDeletionPolicy,
            )
            every { clubMemberRepository.save(any()) } answers { firstArg() }
        }

        describe("setInitialCardinals") {
            val club = ClubTestFixture.createClub()
            val member = ClubMemberTestFixture.createActiveMember(club = club)

            context("복수 기수를 최초 설정하는 경우") {
                it("요청 기수 수만큼 ClubMemberCardinal이 저장되고, 각 기수의 세션에 출석이 초기화된다") {
                    val cardinal30 =
                        CardinalTestFixture.createCardinal(
                            id = 1L,
                            club = club,
                            cardinalNumber = 30,
                        )
                    val cardinal31 =
                        CardinalTestFixture.createCardinal(
                            id = 2L,
                            club = club,
                            cardinalNumber = 31,
                        )
                    every { clubMemberPolicy.getActiveMemberWithLock(1L, 10L) } returns member
                    every { clubMemberCardinalRepository.existsByClubMember(member) } returns false
                    every { cardinalReader.findByClubIdAndCardinalNumber(1L, 30) } returns cardinal30
                    every { cardinalReader.findByClubIdAndCardinalNumber(1L, 31) } returns cardinal31
                    every { clubMemberCardinalRepository.saveAll(any<List<ClubMemberCardinal>>()) } answers
                        { firstArg() }

                    useCase.setInitialCardinals(1L, 10L, ClubMemberCardinalSetRequest(cardinals = listOf(30, 31)))

                    verify(exactly = 1) {
                        clubMemberCardinalRepository.saveAll(
                            match<List<ClubMemberCardinal>> {
                                it.size ==
                                    2
                            },
                        )
                    }
                    verify(exactly = 1) {
                        attendanceInitializer.initializeForMemberCardinals(1L, member, listOf(cardinal30, cardinal31))
                    }
                }
            }

            context("기수를 설정하는 경우") {
                it("ClubMemberCardinal을 저장하고 출석 초기화를 요청한다") {
                    val cardinal =
                        CardinalTestFixture.createCardinal(
                            id = 1L,
                            club = club,
                            cardinalNumber = 30,
                        )

                    every { clubMemberPolicy.getActiveMemberWithLock(1L, 10L) } returns member
                    every { clubMemberCardinalRepository.existsByClubMember(member) } returns false
                    every { cardinalReader.findByClubIdAndCardinalNumber(1L, 30) } returns cardinal
                    every { clubMemberCardinalRepository.saveAll(any<List<ClubMemberCardinal>>()) } answers
                        { firstArg() }

                    useCase.setInitialCardinals(1L, 10L, ClubMemberCardinalSetRequest(cardinals = listOf(30)))

                    verify(exactly = 1) {
                        clubMemberCardinalRepository.saveAll(
                            match<List<ClubMemberCardinal>> {
                                it.size ==
                                    1
                            },
                        )
                    }
                    verify(exactly = 1) {
                        attendanceInitializer.initializeForMemberCardinals(1L, member, listOf(cardinal))
                    }
                }
            }

            context("요청에 중복 기수가 포함된 경우") {
                it("중복을 제거하고 1개만 저장한다") {
                    val cardinal =
                        CardinalTestFixture.createCardinal(
                            id = 1L,
                            club = club,
                            cardinalNumber = 30,
                        )

                    every { clubMemberPolicy.getActiveMemberWithLock(1L, 10L) } returns member
                    every { clubMemberCardinalRepository.existsByClubMember(member) } returns false
                    every { cardinalReader.findByClubIdAndCardinalNumber(1L, 30) } returns cardinal
                    every { clubMemberCardinalRepository.saveAll(any<List<ClubMemberCardinal>>()) } answers
                        { firstArg() }

                    useCase.setInitialCardinals(1L, 10L, ClubMemberCardinalSetRequest(cardinals = listOf(30, 30)))

                    verify(exactly = 1) {
                        clubMemberCardinalRepository.saveAll(
                            match<List<ClubMemberCardinal>> {
                                it.size ==
                                    1
                            },
                        )
                    }
                    verify(exactly = 1) {
                        attendanceInitializer.initializeForMemberCardinals(1L, member, listOf(cardinal))
                    }
                }
            }

            context("이미 기수가 설정된 멤버가 재설정을 시도하는 경우") {
                it("CardinalAlreadySetException이 발생한다") {
                    every { clubMemberPolicy.getActiveMemberWithLock(1L, 10L) } returns member
                    every { clubMemberCardinalRepository.existsByClubMember(member) } returns true

                    shouldThrow<CardinalAlreadySetException> {
                        useCase.setInitialCardinals(1L, 10L, ClubMemberCardinalSetRequest(cardinals = listOf(31)))
                    }

                    verify(exactly = 0) { clubMemberCardinalRepository.saveAll(any<List<ClubMemberCardinal>>()) }
                }
            }

            context("존재하지 않는 기수를 요청하는 경우") {
                it("CardinalNotFoundException이 발생한다") {
                    every { clubMemberPolicy.getActiveMemberWithLock(1L, 10L) } returns member
                    every { clubMemberCardinalRepository.existsByClubMember(member) } returns false
                    every { cardinalReader.findByClubIdAndCardinalNumber(1L, 99) } returns null

                    shouldThrow<CardinalNotFoundException> {
                        useCase.setInitialCardinals(1L, 10L, ClubMemberCardinalSetRequest(cardinals = listOf(99)))
                    }

                    verify(exactly = 0) { clubMemberCardinalRepository.saveAll(any<List<ClubMemberCardinal>>()) }
                }
            }
        }

        describe("leave") {
            it("LEAD 멤버가 탈퇴를 시도하면 예외가 발생한다") {
                val leadMember = ClubMemberTestFixture.createLeadMember()
                every { clubMemberPolicy.getActiveMemberWithLock(1L, 10L) } returns leadMember

                shouldThrow<CannotLeaveAsLeadException> {
                    useCase.leave(1L, 10L)
                }

                verify(exactly = 0) { clubActivityDeletionPolicy.markMemberActivitiesDeleted(any(), any()) }
            }

            it("일반 멤버가 탈퇴하면 활동 삭제 정책을 적용하고 LEFT 상태로 전환된다") {
                val member = ClubMemberTestFixture.createActiveMember()
                val now = LocalDateTime.now(clock)
                every { clubMemberPolicy.getActiveMemberWithLock(1L, 10L) } returns member
                justRun { clubActivityDeletionPolicy.markMemberActivitiesDeleted(eq(member), any()) }

                useCase.leave(1L, 10L)

                member.memberStatus shouldBe MemberStatus.LEFT
                member.leftAt shouldBe now
                verify(exactly = 1) { clubActivityDeletionPolicy.markMemberActivitiesDeleted(eq(member), now) }
            }
        }

        describe("join") {
            context("이미 USER로 1개 동아리에 가입한 사용자가 가입 시도하는 경우") {
                it("ClubJoinLimitExceededException이 발생한다") {
                    val targetClub = ClubTestFixture.createClub(code = "JOIN-CODE")
                    val user = UserTestFixture.createRegisteredUser()

                    every { clubRepository.getClubById(1L) } returns targetClub
                    every { userReader.getByIdWithLock(10L) } returns user
                    every { clubMemberRepository.findByClubIdAndUserId(1L, 10L) } returns null
                    every { clubJoinPolicy.validateJoinLimit(10L) } throws ClubJoinLimitExceededException()

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
                    val user = UserTestFixture.createRegisteredUser()

                    every { clubRepository.getClubById(1L) } returns targetClub
                    every { userReader.getByIdWithLock(10L) } returns user
                    every { clubMemberRepository.findByClubIdAndUserId(1L, 10L) } returns null
                    justRun { clubJoinPolicy.validateJoinLimit(10L) }

                    useCase.join(
                        clubId = 1L,
                        userId = 10L,
                        request = ClubJoinRequest(code = "JOIN-CODE"),
                    )

                    verify(exactly = 1) { clubMemberRepository.save(any()) }
                }
            }

            context("탈퇴 사용자가 가입 시도하는 경우") {
                it("UserInActiveException이 발생하고 가입 처리를 진행하지 않는다") {
                    val targetClub = ClubTestFixture.createClub(code = "JOIN-CODE")
                    val user =
                        UserTestFixture
                            .createRegisteredUser()
                            .apply { leave(LocalDateTime.of(2026, 6, 12, 12, 0)) }

                    every { clubRepository.getClubById(1L) } returns targetClub
                    every { userReader.getByIdWithLock(10L) } returns user

                    shouldThrow<UserInActiveException> {
                        useCase.join(
                            clubId = 1L,
                            userId = 10L,
                            request = ClubJoinRequest(code = "JOIN-CODE"),
                        )
                    }

                    verify(exactly = 0) { clubMemberRepository.findByClubIdAndUserId(any(), any()) }
                    verify(exactly = 0) { clubJoinPolicy.validateJoinLimit(any()) }
                    verify(exactly = 0) { clubMemberRepository.save(any()) }
                }
            }
        }
    })
