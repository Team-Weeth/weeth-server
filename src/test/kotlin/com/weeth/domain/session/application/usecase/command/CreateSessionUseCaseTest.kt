package com.weeth.domain.session.application.usecase.command

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.cardinal.application.exception.CardinalNotFoundException
import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.cardinal.fixture.CardinalTestFixture
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.repository.ClubMemberCardinalReader
import com.weeth.domain.club.domain.repository.ClubReader
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.session.application.dto.request.SessionCreateRequest
import com.weeth.domain.session.application.exception.RecurrenceEndDateBeforeStartException
import com.weeth.domain.session.application.exception.RecurrenceEndDateExceedsMaxException
import com.weeth.domain.session.application.exception.RecurrenceEndDateRequiredException
import com.weeth.domain.session.application.mapper.SessionMapper
import com.weeth.domain.session.domain.entity.Session
import com.weeth.domain.session.domain.entity.SessionGroup
import com.weeth.domain.session.domain.enums.RecurrenceType
import com.weeth.domain.session.domain.repository.SessionGroupRepository
import com.weeth.domain.session.domain.repository.SessionRepository
import com.weeth.domain.session.domain.service.RecurringSessionPolicy
import com.weeth.domain.session.fixture.SessionTestFixture
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalDateTime

class CreateSessionUseCaseTest :
    DescribeSpec({
        val sessionRepository = mockk<SessionRepository>()
        val attendanceRepository = mockk<AttendanceRepository>()
        val sessionGroupRepository = mockk<SessionGroupRepository>()
        val userReader = mockk<UserReader>()
        val cardinalReader = mockk<CardinalReader>()
        val clubReader = mockk<ClubReader>()
        val clubMemberCardinalReader = mockk<ClubMemberCardinalReader>()
        val clubPermissionPolicy = mockk<ClubPermissionPolicy>(relaxed = true)
        val recurringSessionPolicy = RecurringSessionPolicy()
        val sessionMapper = SessionMapper(recurringSessionPolicy)

        val useCase =
            CreateSessionUseCase(
                sessionRepository = sessionRepository,
                attendanceRepository = attendanceRepository,
                sessionGroupRepository = sessionGroupRepository,
                userReader = userReader,
                cardinalReader = cardinalReader,
                sessionMapper = sessionMapper,
                clubReader = clubReader,
                clubMemberCardinalReader = clubMemberCardinalReader,
                clubPermissionPolicy = clubPermissionPolicy,
                recurringSessionPolicy = recurringSessionPolicy,
            )

        val clubId = 1L
        val userId = 10L
        val club = ClubTestFixture.createClub(id = clubId)
        val user = UserTestFixture.createActiveUser1()
        val cardinal = CardinalTestFixture.createCardinal(cardinalNumber = 1, year = 2026, semester = 1, club = club)

        beforeTest {
            clearMocks(
                sessionRepository,
                attendanceRepository,
                sessionGroupRepository,
                userReader,
                cardinalReader,
                clubReader,
                clubMemberCardinalReader,
                clubPermissionPolicy,
            )
            every { clubReader.getClubById(clubId) } returns club
            every { userReader.getById(userId) } returns user
            every { cardinalReader.findByClubIdAndCardinalNumber(clubId, 1) } returns cardinal
            every { clubMemberCardinalReader.findAllByClubIdAndCardinalNumber(clubId, 1, MemberStatus.ACTIVE) } returns
                emptyList()
            every { sessionRepository.save(any()) } answers { firstArg() }
            every { sessionRepository.saveAll(any<List<Session>>()) } answers { firstArg() }
            every { sessionGroupRepository.save(any()) } answers { firstArg() }
            every { attendanceRepository.saveAll(any<List<Attendance>>()) } answers { firstArg() }
        }

        describe("create") {
            context("단일 세션 생성 (recurrenceType = null)") {
                it("세션 1개와 출석 레코드를 생성한다") {
                    val request =
                        SessionCreateRequest(
                            title = "1차 정기모임",
                            content = "OT",
                            location = "공학관 401호",
                            cardinal = 1,
                            start = LocalDateTime.of(2026, 4, 1, 10, 0),
                            end = LocalDateTime.of(2026, 4, 1, 12, 0),
                            recurrenceType = null,
                            recurrenceEndDate = null,
                        )

                    useCase.create(clubId, request, userId)

                    verify(exactly = 1) { sessionRepository.save(any()) }
                    verify(exactly = 0) { sessionGroupRepository.save(any()) }
                    verify(exactly = 0) { sessionRepository.saveAll(any<List<Session>>()) }
                }
            }

            context("반복 세션 생성 (WEEKLY)") {
                it("주간 반복 세션들이 올바르게 생성된다") {
                    val request =
                        SessionCreateRequest(
                            title = "주간 스터디",
                            content = null,
                            location = null,
                            cardinal = 1,
                            start = LocalDateTime.of(2026, 4, 1, 14, 0), // 수요일
                            end = LocalDateTime.of(2026, 4, 1, 16, 0),
                            recurrenceType = RecurrenceType.WEEKLY,
                            recurrenceEndDate = LocalDate.of(2026, 4, 22), // 4주차 수요일
                        )
                    val sessionsSlot = slot<List<Session>>()

                    every { sessionRepository.saveAll(capture(sessionsSlot)) } answers { firstArg() }

                    useCase.create(clubId, request, userId)

                    verify(exactly = 1) { sessionGroupRepository.save(any()) }
                    sessionsSlot.captured.size shouldBe 4
                }

                it("멤버가 있으면 세션 수 × 멤버 수 만큼 출석 레코드가 생성된다") {
                    val member = ClubMemberTestFixture.createActiveMember(club = club)
                    val memberCardinal = mockk<com.weeth.domain.club.domain.entity.ClubMemberCardinal>(relaxed = true)
                    every { memberCardinal.clubMember } returns member
                    every {
                        clubMemberCardinalReader.findAllByClubIdAndCardinalNumber(clubId, 1, MemberStatus.ACTIVE)
                    } returns listOf(memberCardinal)

                    val request =
                        SessionCreateRequest(
                            title = "주간 스터디",
                            content = null,
                            location = null,
                            cardinal = 1,
                            start = LocalDateTime.of(2026, 4, 1, 14, 0),
                            end = LocalDateTime.of(2026, 4, 1, 16, 0),
                            recurrenceType = RecurrenceType.WEEKLY,
                            recurrenceEndDate = LocalDate.of(2026, 4, 15), // 3주
                        )
                    val attendancesSlot = slot<List<Attendance>>()

                    every { attendanceRepository.saveAll(capture(attendancesSlot)) } answers { firstArg() }

                    useCase.create(clubId, request, userId)

                    // 3주 × 1명 = 3개 출석 레코드
                    attendancesSlot.captured.size shouldBe 3
                }
            }

            context("반복 세션 생성 (MONTHLY)") {
                it("월간 반복 세션들이 올바르게 생성된다") {
                    val request =
                        SessionCreateRequest(
                            title = "월례 회의",
                            content = null,
                            location = null,
                            cardinal = 1,
                            start = LocalDateTime.of(2026, 1, 31, 10, 0),
                            end = LocalDateTime.of(2026, 1, 31, 12, 0),
                            recurrenceType = RecurrenceType.MONTHLY,
                            recurrenceEndDate = LocalDate.of(2026, 4, 30),
                        )
                    val sessionsSlot = slot<List<Session>>()

                    every { sessionRepository.saveAll(capture(sessionsSlot)) } answers { firstArg() }

                    useCase.create(clubId, request, userId)

                    val sessions = sessionsSlot.captured
                    sessions.size shouldBe 4

                    sessions[0].start.toLocalDate() shouldBe LocalDate.of(2026, 1, 31)
                    sessions[1].start.toLocalDate() shouldBe LocalDate.of(2026, 2, 28)
                    sessions[2].start.toLocalDate() shouldBe LocalDate.of(2026, 3, 31)
                    sessions[3].start.toLocalDate() shouldBe LocalDate.of(2026, 4, 30)
                }
            }

            context("자정을 넘는 반복 세션 (22:00~02:00)") {
                it("end 날짜가 start 다음날로 설정된다") {
                    val request =
                        SessionCreateRequest(
                            title = "야간 스터디",
                            content = null,
                            location = null,
                            cardinal = 1,
                            start = LocalDateTime.of(2026, 4, 1, 22, 0),
                            end = LocalDateTime.of(2026, 4, 2, 2, 0), // 다음날 새벽 2시
                            recurrenceType = RecurrenceType.WEEKLY,
                            recurrenceEndDate = LocalDate.of(2026, 4, 15),
                        )
                    val sessionsSlot = slot<List<Session>>()

                    every { sessionRepository.saveAll(capture(sessionsSlot)) } answers { firstArg() }

                    useCase.create(clubId, request, userId)

                    val sessions = sessionsSlot.captured
                    sessions.size shouldBe 3

                    // 각 세션의 start는 해당 날짜 22시, end는 다음날 02시
                    sessions[0].start shouldBe LocalDateTime.of(2026, 4, 1, 22, 0)
                    sessions[0].end shouldBe LocalDateTime.of(2026, 4, 2, 2, 0)
                    sessions[1].start shouldBe LocalDateTime.of(2026, 4, 8, 22, 0)
                    sessions[1].end shouldBe LocalDateTime.of(2026, 4, 9, 2, 0)
                    sessions[2].start shouldBe LocalDateTime.of(2026, 4, 15, 22, 0)
                    sessions[2].end shouldBe LocalDateTime.of(2026, 4, 16, 2, 0)
                }
            }

            context("검증 실패") {
                it("존재하지 않는 기수이면 예외를 던진다") {
                    every { cardinalReader.findByClubIdAndCardinalNumber(clubId, 99) } returns null

                    val request =
                        SessionCreateRequest(
                            title = "세션",
                            content = null,
                            location = null,
                            cardinal = 99,
                            start = LocalDateTime.of(2026, 4, 1, 10, 0),
                            end = LocalDateTime.of(2026, 4, 1, 12, 0),
                            recurrenceType = null,
                            recurrenceEndDate = null,
                        )

                    shouldThrow<CardinalNotFoundException> {
                        useCase.create(clubId, request, userId)
                    }
                }

                it("반복 타입이 있는데 종료일이 없으면 예외를 던진다") {
                    val request =
                        SessionCreateRequest(
                            title = "반복 세션",
                            content = null,
                            location = null,
                            cardinal = 1,
                            start = LocalDateTime.of(2026, 4, 1, 10, 0),
                            end = LocalDateTime.of(2026, 4, 1, 12, 0),
                            recurrenceType = RecurrenceType.WEEKLY,
                            recurrenceEndDate = null,
                        )

                    shouldThrow<RecurrenceEndDateRequiredException> {
                        useCase.create(clubId, request, userId)
                    }
                }

                it("반복 종료일이 시작일보다 이전이면 예외를 던진다") {
                    val request =
                        SessionCreateRequest(
                            title = "반복 세션",
                            content = null,
                            location = null,
                            cardinal = 1,
                            start = LocalDateTime.of(2026, 4, 10, 10, 0),
                            end = LocalDateTime.of(2026, 4, 10, 12, 0),
                            recurrenceType = RecurrenceType.WEEKLY,
                            recurrenceEndDate = LocalDate.of(2026, 4, 1),
                        )

                    shouldThrow<RecurrenceEndDateBeforeStartException> {
                        useCase.create(clubId, request, userId)
                    }
                }

                it("반복 종료일이 시작일 기준 1년을 초과하면 예외를 던진다") {
                    val request =
                        SessionCreateRequest(
                            title = "반복 세션",
                            content = null,
                            location = null,
                            cardinal = 1,
                            start = LocalDateTime.of(2026, 4, 1, 10, 0),
                            end = LocalDateTime.of(2026, 4, 1, 12, 0),
                            recurrenceType = RecurrenceType.WEEKLY,
                            recurrenceEndDate = LocalDate.of(2027, 4, 2), // 1년 + 1일
                        )

                    shouldThrow<RecurrenceEndDateExceedsMaxException> {
                        useCase.create(clubId, request, userId)
                    }
                }

                it("반복 종료일이 시작일 기준 정확히 1년이면 성공한다") {
                    val request =
                        SessionCreateRequest(
                            title = "반복 세션",
                            content = null,
                            location = null,
                            cardinal = 1,
                            start = LocalDateTime.of(2026, 4, 1, 10, 0),
                            end = LocalDateTime.of(2026, 4, 1, 12, 0),
                            recurrenceType = RecurrenceType.WEEKLY,
                            recurrenceEndDate = LocalDate.of(2027, 4, 1), // 정확히 1년
                        )

                    useCase.create(clubId, request, userId)

                    verify(exactly = 1) { sessionGroupRepository.save(any()) }
                }
            }
        }
    })
