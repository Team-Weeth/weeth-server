package com.weeth.domain.schedule.application.usecase

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.entity.Session
import com.weeth.domain.attendance.domain.entity.enums.AttendanceStatus
import com.weeth.domain.attendance.domain.service.AttendanceDeleteService
import com.weeth.domain.attendance.domain.service.AttendanceGetService
import com.weeth.domain.attendance.domain.service.AttendanceSaveService
import com.weeth.domain.attendance.domain.service.AttendanceUpdateService
import com.weeth.domain.schedule.application.dto.request.ScheduleSaveRequest
import com.weeth.domain.schedule.application.dto.response.SessionInfoResponse
import com.weeth.domain.schedule.application.dto.response.SessionInfosResponse
import com.weeth.domain.schedule.application.dto.response.SessionResponse
import com.weeth.domain.schedule.application.mapper.SessionMapper
import com.weeth.domain.schedule.domain.entity.enums.Type
import com.weeth.domain.schedule.domain.service.MeetingDeleteService
import com.weeth.domain.schedule.domain.service.MeetingGetService
import com.weeth.domain.schedule.domain.service.MeetingSaveService
import com.weeth.domain.schedule.domain.service.MeetingUpdateService
import com.weeth.domain.schedule.fixture.ScheduleTestFixture
import com.weeth.domain.user.domain.entity.Cardinal
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.entity.enums.Role
import com.weeth.domain.user.domain.service.CardinalGetService
import com.weeth.domain.user.domain.service.UserGetService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityManager
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime

class MeetingUseCaseImplTest :
    DescribeSpec({
        val meetingGetService = mockk<MeetingGetService>()
        val sessionMapper = mockk<SessionMapper>()
        val meetingSaveService = mockk<MeetingSaveService>(relaxUnitFun = true)
        val userGetService = mockk<UserGetService>()
        val meetingUpdateService = mockk<MeetingUpdateService>(relaxUnitFun = true)
        val meetingDeleteService = mockk<MeetingDeleteService>(relaxUnitFun = true)
        val attendanceGetService = mockk<AttendanceGetService>()
        val attendanceSaveService = mockk<AttendanceSaveService>(relaxUnitFun = true)
        val attendanceDeleteService = mockk<AttendanceDeleteService>(relaxUnitFun = true)
        val attendanceUpdateService = mockk<AttendanceUpdateService>(relaxUnitFun = true)
        val cardinalGetService = mockk<CardinalGetService>()
        val em = mockk<EntityManager>(relaxUnitFun = true)

        val useCase =
            MeetingUseCaseImpl(
                meetingGetService,
                sessionMapper,
                meetingSaveService,
                userGetService,
                meetingUpdateService,
                meetingDeleteService,
                attendanceGetService,
                attendanceSaveService,
                attendanceDeleteService,
                attendanceUpdateService,
                cardinalGetService,
            )

        beforeSpec {
            ReflectionTestUtils.setField(useCase, "em", em)
        }

        beforeTest {
            clearMocks(
                meetingGetService,
                sessionMapper,
                meetingSaveService,
                userGetService,
                meetingUpdateService,
                meetingDeleteService,
                attendanceGetService,
                attendanceSaveService,
                attendanceDeleteService,
                attendanceUpdateService,
                cardinalGetService,
                em,
            )
        }

        describe("find(userId, sessionId)") {
            val sessionId = 1L
            val userId = 10L
            val session = ScheduleTestFixture.createSession(id = sessionId)

            context("ADMIN 유저일 때") {
                it("toAdminResponse로 매핑한다") {
                    val adminUser = mockk<User>()
                    every { adminUser.role } returns Role.ADMIN
                    every { userGetService.find(userId) } returns adminUser
                    every { meetingGetService.find(sessionId) } returns session
                    val adminResponse = mockk<SessionResponse>()
                    every { sessionMapper.toAdminResponse(session) } returns adminResponse

                    val result = useCase.find(userId, sessionId)

                    result shouldBe adminResponse
                    verify { sessionMapper.toAdminResponse(session) }
                }
            }

            context("일반 유저일 때") {
                it("toResponse(session)으로 매핑한다 (코드 미노출)") {
                    val normalUser = mockk<User>()
                    every { normalUser.role } returns Role.USER
                    every { userGetService.find(userId) } returns normalUser
                    every { meetingGetService.find(sessionId) } returns session
                    val normalResponse = mockk<SessionResponse>()
                    every { sessionMapper.toResponse(session) } returns normalResponse

                    val result = useCase.find(userId, sessionId)

                    result shouldBe normalResponse
                    verify { sessionMapper.toResponse(session) }
                }
            }
        }

        describe("find(cardinal)") {
            it("이번 주 세션이 있으면 thisWeek에 포함된다") {
                val now = LocalDateTime.now()
                val thisWeekSession =
                    ScheduleTestFixture.createSession(
                        id = 1L,
                        title = "This Week",
                        start = now,
                        end = now.plusHours(2),
                    )
                val lastWeekSession =
                    ScheduleTestFixture.createSession(
                        id = 2L,
                        title = "Last Week",
                        start = now.minusDays(14),
                        end = now.minusDays(14).plusHours(2),
                    )
                val thisWeekInfo = SessionInfoResponse(1L, 1, "This Week", now)
                val lastWeekInfo = SessionInfoResponse(2L, 1, "Last Week", now.minusDays(14))
                val expectedInfos = SessionInfosResponse(thisWeekInfo, listOf(thisWeekInfo, lastWeekInfo))

                every { meetingGetService.findMeetingByCardinal(1) } returns listOf(thisWeekSession, lastWeekSession)
                every { sessionMapper.toInfos(thisWeekSession, any()) } returns expectedInfos

                val result = useCase.find(1)

                result.thisWeek shouldNotBe null
                result.thisWeek!!.title shouldBe "This Week"
            }
        }

        describe("save") {
            it("세션 저장 후 해당 기수 전체 유저에게 출석을 생성한다") {
                val userId = 10L
                val user = mockk<User>()
                val cardinal = mockk<Cardinal>()
                val userList = listOf(mockk<User>(), mockk<User>(), mockk<User>())
                val session = ScheduleTestFixture.createSession()
                val dto =
                    ScheduleSaveRequest(
                        title = "Title",
                        content = "Content",
                        location = "Location",
                        type = Type.MEETING,
                        cardinal = 1,
                        start = LocalDateTime.of(2026, 3, 1, 10, 0),
                        end = LocalDateTime.of(2026, 3, 1, 12, 0),
                    )

                every { userGetService.find(userId) } returns user
                every { cardinalGetService.findByUserSide(1) } returns cardinal
                every { userGetService.findAllByCardinal(cardinal) } returns userList
                every { sessionMapper.toEntity(dto, user) } returns session

                useCase.save(dto, userId)

                verify(exactly = 1) { meetingSaveService.save(session) }
                verify(exactly = 1) { attendanceSaveService.saveAll(userList, session) }
            }
        }

        describe("delete") {
            it("출석 통계 롤백 후 출석 삭제 → 세션 삭제 순서로 처리한다") {
                val sessionId = 1L
                val session = ScheduleTestFixture.createSession(id = sessionId)
                val attendance1 = mockk<Attendance>()
                val attendance2 = mockk<Attendance>()
                val attendances = listOf(attendance1, attendance2)

                every { meetingGetService.find(sessionId) } returns session
                every { attendanceGetService.findAllByMeeting(session) } returns attendances

                useCase.delete(sessionId)

                verify(ordering = io.mockk.Ordering.ORDERED) {
                    attendanceUpdateService.updateUserAttendanceByStatus(attendances)
                    em.flush()
                    em.clear()
                    attendanceDeleteService.deleteAll(session)
                    meetingDeleteService.delete(session)
                }
            }
        }
    })
