package com.weeth.domain.club.application.usecase.command

import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.cardinal.application.exception.CardinalNotFoundException
import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.cardinal.fixture.CardinalTestFixture
import com.weeth.domain.club.application.dto.request.ClubJoinRequest
import com.weeth.domain.club.application.dto.request.ClubMemberCardinalSetRequest
import com.weeth.domain.club.application.exception.CardinalAlreadySetException
import com.weeth.domain.club.application.exception.ClubCantJoinException
import com.weeth.domain.club.domain.entity.ClubMemberCardinal
import com.weeth.domain.club.domain.repository.ClubMemberCardinalRepository
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.repository.ClubRepository
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.session.domain.repository.SessionReader
import com.weeth.domain.session.fixture.SessionTestFixture
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class ManageClubMemberUseCaseTest :
    DescribeSpec({
        val clubRepository = mockk<ClubRepository>()
        val clubMemberRepository = mockk<ClubMemberRepository>()
        val clubMemberCardinalRepository = mockk<ClubMemberCardinalRepository>(relaxed = true)
        val cardinalReader = mockk<CardinalReader>()
        val sessionReader = mockk<SessionReader>()
        val attendanceRepository = mockk<AttendanceRepository>(relaxed = true)
        val userReader = mockk<UserReader>()
        val clubMemberPolicy = mockk<ClubMemberPolicy>()

        val useCase =
            ManageClubMemberUsecase(
                clubRepository = clubRepository,
                clubMemberRepository = clubMemberRepository,
                clubMemberCardinalRepository = clubMemberCardinalRepository,
                cardinalReader = cardinalReader,
                sessionReader = sessionReader,
                attendanceRepository = attendanceRepository,
                userReader = userReader,
                clubMemberPolicy = clubMemberPolicy,
            )

        beforeTest {
            clearMocks(
                clubRepository,
                clubMemberRepository,
                clubMemberCardinalRepository,
                cardinalReader,
                sessionReader,
                attendanceRepository,
                userReader,
                clubMemberPolicy,
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
                            year = 2024,
                            semester = 1,
                        )
                    val cardinal31 =
                        CardinalTestFixture.createCardinal(
                            id = 2L,
                            club = club,
                            cardinalNumber = 31,
                            year = 2024,
                            semester = 2,
                        )
                    val session30 = SessionTestFixture.createSession(club = club, cardinal = 30)
                    val session31 = SessionTestFixture.createSession(club = club, cardinal = 31)

                    every { clubMemberPolicy.getActiveMemberWithLock(1L, 10L) } returns member
                    every { clubMemberCardinalRepository.findAllByClubMember(member) } returns emptyList()
                    every { cardinalReader.findByClubIdAndCardinalNumber(1L, 30) } returns cardinal30
                    every { cardinalReader.findByClubIdAndCardinalNumber(1L, 31) } returns cardinal31
                    every { clubMemberCardinalRepository.saveAll(any<List<ClubMemberCardinal>>()) } answers
                        { firstArg() }
                    every {
                        sessionReader.findAllByClubIdAndCardinalIn(1L, listOf(30, 31))
                    } returns listOf(session30, session31)

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
                        attendanceRepository.saveAll(
                            match<List<com.weeth.domain.attendance.domain.entity.Attendance>> {
                                it.size ==
                                    2
                            },
                        )
                    }
                }
            }

            context("세션이 없는 기수를 설정하는 경우") {
                it("ClubMemberCardinal만 저장되고 출석은 초기화되지 않는다") {
                    val cardinal =
                        CardinalTestFixture.createCardinal(
                            id = 1L,
                            club = club,
                            cardinalNumber = 30,
                            year = 2024,
                            semester = 1,
                        )

                    every { clubMemberPolicy.getActiveMemberWithLock(1L, 10L) } returns member
                    every { clubMemberCardinalRepository.findAllByClubMember(member) } returns emptyList()
                    every { cardinalReader.findByClubIdAndCardinalNumber(1L, 30) } returns cardinal
                    every { clubMemberCardinalRepository.saveAll(any<List<ClubMemberCardinal>>()) } answers
                        { firstArg() }
                    every { sessionReader.findAllByClubIdAndCardinalIn(1L, listOf(30)) } returns emptyList()

                    useCase.setInitialCardinals(1L, 10L, ClubMemberCardinalSetRequest(cardinals = listOf(30)))

                    verify(exactly = 1) {
                        clubMemberCardinalRepository.saveAll(
                            match<List<ClubMemberCardinal>> {
                                it.size ==
                                    1
                            },
                        )
                    }
                    verify(
                        exactly = 0,
                    ) {
                        attendanceRepository.saveAll(
                            any<List<com.weeth.domain.attendance.domain.entity.Attendance>>(),
                        )
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
                            year = 2024,
                            semester = 1,
                        )

                    every { clubMemberPolicy.getActiveMemberWithLock(1L, 10L) } returns member
                    every { clubMemberCardinalRepository.findAllByClubMember(member) } returns emptyList()
                    every { cardinalReader.findByClubIdAndCardinalNumber(1L, 30) } returns cardinal
                    every { clubMemberCardinalRepository.saveAll(any<List<ClubMemberCardinal>>()) } answers
                        { firstArg() }
                    every { sessionReader.findAllByClubIdAndCardinalIn(1L, listOf(30)) } returns emptyList()

                    useCase.setInitialCardinals(1L, 10L, ClubMemberCardinalSetRequest(cardinals = listOf(30, 30)))

                    verify(exactly = 1) {
                        clubMemberCardinalRepository.saveAll(
                            match<List<ClubMemberCardinal>> {
                                it.size ==
                                    1
                            },
                        )
                    }
                }
            }

            context("이미 기수가 설정된 멤버가 재설정을 시도하는 경우") {
                it("CardinalAlreadySetException이 발생한다") {
                    val cardinal =
                        CardinalTestFixture.createCardinal(
                            id = 1L,
                            club = club,
                            cardinalNumber = 30,
                            year = 2024,
                            semester = 1,
                        )
                    val existingMemberCardinal = ClubMemberCardinal.create(member, cardinal)

                    every { clubMemberPolicy.getActiveMemberWithLock(1L, 10L) } returns member
                    every { clubMemberCardinalRepository.findAllByClubMember(member) } returns
                        listOf(existingMemberCardinal)

                    shouldThrow<CardinalAlreadySetException> {
                        useCase.setInitialCardinals(1L, 10L, ClubMemberCardinalSetRequest(cardinals = listOf(31)))
                    }

                    verify(exactly = 0) { clubMemberCardinalRepository.saveAll(any<List<ClubMemberCardinal>>()) }
                }
            }

            context("존재하지 않는 기수를 요청하는 경우") {
                it("CardinalNotFoundException이 발생한다") {
                    every { clubMemberPolicy.getActiveMemberWithLock(1L, 10L) } returns member
                    every { clubMemberCardinalRepository.findAllByClubMember(member) } returns emptyList()
                    every { cardinalReader.findByClubIdAndCardinalNumber(1L, 99) } returns null

                    shouldThrow<CardinalNotFoundException> {
                        useCase.setInitialCardinals(1L, 10L, ClubMemberCardinalSetRequest(cardinals = listOf(99)))
                    }

                    verify(exactly = 0) { clubMemberCardinalRepository.saveAll(any<List<ClubMemberCardinal>>()) }
                }
            }
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
                    every { userReader.getByIdWithLock(10L) } returns user
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
