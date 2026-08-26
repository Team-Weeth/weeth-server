package com.weeth.domain.schedule.application.usecase.query

import com.weeth.domain.attendance.domain.repository.AttendanceReader
import com.weeth.domain.attendance.fixture.AttendanceTestFixture
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.schedule.application.dto.response.EventResponse
import com.weeth.domain.schedule.application.dto.response.ScheduleAttendanceStatus
import com.weeth.domain.schedule.application.dto.response.ScheduleDetailResponse
import com.weeth.domain.schedule.application.dto.response.ScheduleResponse
import com.weeth.domain.schedule.application.exception.EventNotFoundException
import com.weeth.domain.schedule.application.mapper.EventMapper
import com.weeth.domain.schedule.application.mapper.ScheduleMapper
import com.weeth.domain.schedule.domain.enums.Type
import com.weeth.domain.schedule.domain.repository.EventRepository
import com.weeth.domain.schedule.fixture.ScheduleTestFixture
import com.weeth.domain.session.application.exception.SessionNotFoundException
import com.weeth.domain.session.domain.enums.SessionStatus
import com.weeth.domain.session.domain.repository.SessionReader
import com.weeth.domain.session.fixture.SessionTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDateTime

class GetScheduleQueryServiceTest :
    DescribeSpec({
        val eventRepository = mockk<EventRepository>()
        val sessionReader = mockk<SessionReader>()
        val attendanceReader = mockk<AttendanceReader>()
        val clubMemberPolicy = mockk<ClubMemberPolicy>(relaxed = true)
        val scheduleMapper = mockk<ScheduleMapper>()
        val eventMapper = mockk<EventMapper>()
        val queryService =
            GetScheduleQueryService(
                eventRepository,
                sessionReader,
                attendanceReader,
                clubMemberPolicy,
                scheduleMapper,
                eventMapper,
            )

        val clubId = 1L
        val userId = 10L
        val cardinal = 7
        val start = LocalDateTime.of(2026, 12, 1, 0, 0)
        val end = LocalDateTime.of(2026, 12, 31, 23, 59, 59)

        beforeTest {
            clearMocks(eventRepository, sessionReader, attendanceReader, scheduleMapper)
        }

        describe("findMonthly") {
            it("이벤트와 세션을 시작 시간 순으로 합쳐서 반환한다") {
                val event =
                    ScheduleTestFixture.createEvent(
                        id = 1L,
                        cardinal = cardinal,
                        start = LocalDateTime.of(2026, 12, 10, 10, 0),
                        end = LocalDateTime.of(2026, 12, 10, 12, 0),
                    )
                val session =
                    SessionTestFixture.createSession(
                        id = 2L,
                        cardinal = cardinal,
                        start = LocalDateTime.of(2026, 12, 5, 14, 0),
                        end = LocalDateTime.of(2026, 12, 5, 16, 0),
                    )
                val eventResponse =
                    ScheduleResponse(
                        id = 1L,
                        title = "Test Event",
                        start = event.start,
                        end = event.end,
                        type = Type.EVENT,
                        location = "Test Location",
                        cardinal = cardinal,
                    )
                val sessionResponse =
                    ScheduleResponse(
                        id = 2L,
                        title = "Test Session",
                        start = session.start,
                        end = session.end,
                        type = Type.SESSION,
                        location = "Test Location",
                        cardinal = cardinal,
                    )

                every { eventRepository.findByClubIdAndCardinalAndDateRange(clubId, cardinal, start, end) } returns
                    listOf(event)
                every { sessionReader.findAllByClubIdAndCardinalAndStartBetween(clubId, cardinal, start, end) } returns
                    listOf(session)
                every { scheduleMapper.toResponse(event) } returns eventResponse
                every { scheduleMapper.toResponse(session) } returns sessionResponse

                // 세션(12/5)이 이벤트(12/10)보다 앞에 와야 함
                queryService.findMonthly(clubId, userId, cardinal, start, end) shouldBe
                    listOf(sessionResponse, eventResponse)
            }

            it("이벤트만 있으면 이벤트만 반환한다") {
                val event = ScheduleTestFixture.createEvent(id = 1L, cardinal = cardinal)
                val eventResponse =
                    ScheduleResponse(
                        id = 1L,
                        title = "Test Event",
                        start = event.start,
                        end = event.end,
                        type = Type.EVENT,
                        location = "Test Location",
                        cardinal = cardinal,
                    )

                every { eventRepository.findByClubIdAndCardinalAndDateRange(clubId, cardinal, start, end) } returns
                    listOf(event)
                every { sessionReader.findAllByClubIdAndCardinalAndStartBetween(clubId, cardinal, start, end) } returns
                    emptyList()
                every { scheduleMapper.toResponse(event) } returns eventResponse

                queryService.findMonthly(clubId, userId, cardinal, start, end) shouldBe listOf(eventResponse)
            }

            it("세션만 있으면 세션만 반환한다") {
                val session = SessionTestFixture.createSession(id = 1L, cardinal = cardinal)
                val sessionResponse =
                    ScheduleResponse(
                        id = 1L,
                        title = "Test Session",
                        start = session.start,
                        end = session.end,
                        type = Type.SESSION,
                        location = "Test Location",
                        cardinal = cardinal,
                    )

                every { eventRepository.findByClubIdAndCardinalAndDateRange(clubId, cardinal, start, end) } returns
                    emptyList()
                every { sessionReader.findAllByClubIdAndCardinalAndStartBetween(clubId, cardinal, start, end) } returns
                    listOf(session)
                every { scheduleMapper.toResponse(session) } returns sessionResponse

                queryService.findMonthly(clubId, userId, cardinal, start, end) shouldBe listOf(sessionResponse)
            }

            it("해당 기수의 일정이 없으면 빈 목록을 반환한다") {
                every { eventRepository.findByClubIdAndCardinalAndDateRange(clubId, cardinal, start, end) } returns
                    emptyList()
                every { sessionReader.findAllByClubIdAndCardinalAndStartBetween(clubId, cardinal, start, end) } returns
                    emptyList()

                queryService.findMonthly(clubId, userId, cardinal, start, end) shouldBe emptyList()
            }
        }

        describe("findAdminEvents") {
            it("cardinal이 있으면 해당 기수 이벤트만 반환한다") {
                val event = ScheduleTestFixture.createEvent(id = 1L, cardinal = cardinal)
                val eventResponse = mockk<EventResponse>()

                every { eventRepository.findByClubIdAndCardinalAndDateRange(clubId, cardinal, start, end) } returns
                    listOf(event)
                every { eventMapper.toResponse(event) } returns eventResponse

                queryService.findAdminEvents(clubId, userId, cardinal, start, end) shouldBe listOf(eventResponse)
            }

            it("cardinal이 null이면 전체 기수 이벤트를 반환한다") {
                val event1 = ScheduleTestFixture.createEvent(id = 1L, cardinal = 6)
                val event2 = ScheduleTestFixture.createEvent(id = 2L, cardinal = 7)
                val response1 = mockk<EventResponse>()
                val response2 = mockk<EventResponse>()

                every { eventRepository.findByClubIdAndDateRange(clubId, start, end) } returns listOf(event1, event2)
                every { eventMapper.toResponse(event1) } returns response1
                every { eventMapper.toResponse(event2) } returns response2

                queryService.findAdminEvents(clubId, userId, null, start, end) shouldBe listOf(response1, response2)
            }

            it("일정이 없으면 빈 목록을 반환한다") {
                every { eventRepository.findByClubIdAndCardinalAndDateRange(clubId, cardinal, start, end) } returns
                    emptyList()

                queryService.findAdminEvents(clubId, userId, cardinal, start, end) shouldBe emptyList()
            }
        }

        describe("findDetail") {
            val club = ClubTestFixture.createClub(id = clubId)
            val member = ClubMemberTestFixture.createActiveMember(club = club)

            context("EVENT 타입일 때") {
                it("이벤트 상세를 반환한다") {
                    val event = ScheduleTestFixture.createEvent(id = 1L, club = club)
                    val mockResponse = mockk<ScheduleDetailResponse>()

                    every { eventRepository.findByIdOrNull(1L) } returns event
                    every { scheduleMapper.toDetailResponse(event) } returns mockResponse

                    queryService.findDetail(clubId, userId, 1L, Type.EVENT) shouldBe mockResponse
                }

                it("이벤트가 없으면 EventNotFoundException을 던진다") {
                    every { eventRepository.findByIdOrNull(99L) } returns null

                    shouldThrow<EventNotFoundException> {
                        queryService.findDetail(clubId, userId, 99L, Type.EVENT)
                    }
                }

                it("이벤트가 다른 클럽 소속이면 EventNotFoundException을 던진다") {
                    val otherClub = ClubTestFixture.createClub(id = 999L)
                    val event = ScheduleTestFixture.createEvent(id = 1L, club = otherClub)

                    every { eventRepository.findByIdOrNull(1L) } returns event

                    shouldThrow<EventNotFoundException> {
                        queryService.findDetail(clubId, userId, 1L, Type.EVENT)
                    }
                }
            }

            context("SESSION 타입일 때") {
                it("출석 완료(COMPLETED)이면 attendedAt과 함께 매퍼를 호출한다") {
                    val session = SessionTestFixture.createSession(id = 1L, club = club)
                    val attendance = AttendanceTestFixture.createAttendance(session, member)
                    attendance.attend()
                    val mockResponse = mockk<ScheduleDetailResponse>()

                    every { sessionReader.getById(1L) } returns session
                    every { attendanceReader.findAllBySession(session) } returns listOf(attendance)
                    every { attendanceReader.findBySessionAndUserId(session, userId) } returns attendance
                    every { scheduleMapper.toDetailResponse(session, listOf(attendance), any(), any()) } returns
                        mockResponse

                    queryService.findDetail(clubId, userId, 1L, Type.SESSION) shouldBe mockResponse
                    verify {
                        scheduleMapper.toDetailResponse(
                            session,
                            listOf(attendance),
                            ScheduleAttendanceStatus.COMPLETED,
                            attendance.modifiedAt,
                        )
                    }
                }

                it("결석(ABSENT)이면 attendedAt 없이 매퍼를 호출한다") {
                    val session = SessionTestFixture.createSession(id = 1L, club = club)
                    val attendance = AttendanceTestFixture.createAttendance(session, member)
                    attendance.absent()

                    every { sessionReader.getById(1L) } returns session
                    every { attendanceReader.findAllBySession(session) } returns listOf(attendance)
                    every { attendanceReader.findBySessionAndUserId(session, userId) } returns attendance
                    every { scheduleMapper.toDetailResponse(session, listOf(attendance), any(), any()) } returns mockk()

                    queryService.findDetail(clubId, userId, 1L, Type.SESSION)
                    verify {
                        scheduleMapper.toDetailResponse(
                            session,
                            listOf(attendance),
                            ScheduleAttendanceStatus.ABSENT,
                            null,
                        )
                    }
                }

                it("출석 예정(UPCOMING)이면 attendedAt 없이 매퍼를 호출한다") {
                    val session =
                        SessionTestFixture.createSession(
                            id = 1L,
                            club = club,
                            start = LocalDateTime.now().plusDays(1),
                            end = LocalDateTime.now().plusDays(1).plusHours(2),
                        )
                    val attendance = AttendanceTestFixture.createAttendance(session, member)

                    every { sessionReader.getById(1L) } returns session
                    every { attendanceReader.findAllBySession(session) } returns listOf(attendance)
                    every { attendanceReader.findBySessionAndUserId(session, userId) } returns attendance
                    every { scheduleMapper.toDetailResponse(session, listOf(attendance), any(), any()) } returns mockk()

                    queryService.findDetail(clubId, userId, 1L, Type.SESSION)
                    verify {
                        scheduleMapper.toDetailResponse(
                            session,
                            listOf(attendance),
                            ScheduleAttendanceStatus.UPCOMING,
                            null,
                        )
                    }
                }

                it("현재 출석 가능(OPEN) 시간이면 OPEN 상태로 매퍼를 호출한다") {
                    val session =
                        SessionTestFixture.createSession(
                            id = 1L,
                            club = club,
                            start = LocalDateTime.now().minusHours(1),
                            end = LocalDateTime.now().plusHours(1),
                            status = SessionStatus.OPEN,
                        )
                    val attendance = AttendanceTestFixture.createAttendance(session, member)

                    every { sessionReader.getById(1L) } returns session
                    every { attendanceReader.findAllBySession(session) } returns listOf(attendance)
                    every { attendanceReader.findBySessionAndUserId(session, userId) } returns attendance
                    every { scheduleMapper.toDetailResponse(session, listOf(attendance), any(), any()) } returns mockk()

                    queryService.findDetail(clubId, userId, 1L, Type.SESSION)
                    verify {
                        scheduleMapper.toDetailResponse(
                            session,
                            listOf(attendance),
                            ScheduleAttendanceStatus.OPEN,
                            null,
                        )
                    }
                }

                it("세션이 없으면 SessionNotFoundException을 던진다") {
                    every { sessionReader.getById(99L) } throws SessionNotFoundException()

                    shouldThrow<SessionNotFoundException> {
                        queryService.findDetail(clubId, userId, 99L, Type.SESSION)
                    }
                }

                it("세션이 다른 클럽 소속이면 SessionNotFoundException을 던진다") {
                    val otherClub = ClubTestFixture.createClub(id = 999L)
                    val session = SessionTestFixture.createSession(id = 1L, club = otherClub)

                    every { sessionReader.getById(1L) } returns session

                    shouldThrow<SessionNotFoundException> {
                        queryService.findDetail(clubId, userId, 1L, Type.SESSION)
                    }
                }
            }
        }
    })
