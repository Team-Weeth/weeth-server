package com.weeth.domain.attendance.application.usecase.query

import com.weeth.domain.attendance.application.dto.response.AttendanceDetailResponse
import com.weeth.domain.attendance.application.dto.response.AttendanceInfoResponse
import com.weeth.domain.attendance.application.dto.response.AttendanceMainResponse
import com.weeth.domain.attendance.application.dto.response.AttendanceResponse
import com.weeth.domain.attendance.application.mapper.AttendanceMapper
import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.attendance.fixture.AttendanceTestFixture.createActiveUserWithAttendances
import com.weeth.domain.attendance.fixture.AttendanceTestFixture.createOneDayMeeting
import com.weeth.domain.schedule.domain.entity.Meeting
import com.weeth.domain.schedule.domain.service.MeetingGetService
import com.weeth.domain.user.domain.entity.Cardinal
import com.weeth.domain.user.domain.entity.enums.Status
import com.weeth.domain.user.domain.service.UserCardinalGetService
import com.weeth.domain.user.domain.service.UserGetService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate

class GetAttendanceQueryServiceTest :
    DescribeSpec({

        val userGetService = mockk<UserGetService>()
        val userCardinalGetService = mockk<UserCardinalGetService>()
        val meetingGetService = mockk<MeetingGetService>()
        val attendanceRepository = mockk<AttendanceRepository>()
        val attendanceMapper = mockk<AttendanceMapper>()

        val queryService =
            GetAttendanceQueryService(
                userGetService,
                userCardinalGetService,
                meetingGetService,
                attendanceRepository,
                attendanceMapper,
            )

        val userId = 10L

        describe("find") {
            it("여러 날짜의 출석 목록 중 시작/종료 날짜가 모두 오늘인 출석정보를 선택") {
                val today = LocalDate.now()

                val meetingYesterday = createOneDayMeeting(today.minusDays(1), 1, 1111, "Yesterday")
                val meetingToday = createOneDayMeeting(today, 1, 2222, "Today")
                val meetingTomorrow = createOneDayMeeting(today.plusDays(1), 1, 3333, "Tomorrow")

                val user =
                    createActiveUserWithAttendances(
                        "이지훈",
                        listOf(meetingYesterday, meetingToday, meetingTomorrow),
                    )

                val expectedTodayAttendance =
                    user.attendances.first {
                        it.meeting == meetingToday
                    }

                val mapped = mockk<AttendanceMainResponse>()

                every { userGetService.find(userId) } returns user
                every { attendanceMapper.toMainResponse(eq(user), eq(expectedTodayAttendance)) } returns mapped

                val actual = queryService.find(userId)

                actual shouldBe mapped
                verify { attendanceMapper.toMainResponse(eq(user), eq(expectedTodayAttendance)) }
            }

            it("시작/종료 날짜가 모두 오늘인 출석이 없다면 mapper.toMainResponse(user, null)을 호출") {
                val today = LocalDate.now()

                val yesterdayMeeting = createOneDayMeeting(today.minusDays(1), 1, 1111, "Yesterday")
                val tomorrowMeeting = createOneDayMeeting(today.plusDays(1), 1, 3333, "Tomorrow")

                val user =
                    createActiveUserWithAttendances(
                        "이지훈",
                        listOf(yesterdayMeeting, tomorrowMeeting),
                    )

                val mapped = mockk<AttendanceMainResponse>()
                every { userGetService.find(userId) } returns user
                every { attendanceMapper.toMainResponse(user, null) } returns mapped

                val actual = queryService.find(userId)

                actual shouldBe mapped
                verify { attendanceMapper.toMainResponse(user, null) }
            }
        }

        describe("findAllDetailsByCurrentCardinal") {
            it("현재 기수만 필터링·정렬하여 Detail 매핑") {
                val today = LocalDate.now()
                val meetingDayMinus1 = createOneDayMeeting(today.minusDays(1), 1, 1111, "D-1")
                val meetingToday = createOneDayMeeting(today, 1, 2222, "D-Day")
                val user = createActiveUserWithAttendances("이지훈", listOf(meetingDayMinus1, meetingToday))

                val userAttendances = user.attendances
                val attendanceFirst = userAttendances[0]
                val attendanceSecond = userAttendances[1]

                every { userGetService.find(userId) } returns user
                val currentCardinal = mockk<Cardinal>()
                every { currentCardinal.cardinalNumber } returns 1
                every { userCardinalGetService.getCurrentCardinal(user) } returns currentCardinal

                val responseFirst = mockk<AttendanceResponse>()
                val responseSecond = mockk<AttendanceResponse>()
                every { attendanceMapper.toResponse(attendanceFirst) } returns responseFirst
                every { attendanceMapper.toResponse(attendanceSecond) } returns responseSecond

                val expectedDetail = mockk<AttendanceDetailResponse>()
                every { attendanceMapper.toDetailResponse(eq(user), any()) } returns expectedDetail

                val actualDetail = queryService.findAllDetailsByCurrentCardinal(userId)

                actualDetail shouldBe expectedDetail
                verify {
                    attendanceMapper.toDetailResponse(
                        eq(user),
                        match { it.size == 2 },
                    )
                }
            }
        }

        describe("findAllAttendanceByMeeting") {
            it("해당 정기모임의 출석 정보를 조회") {
                val meetingId = 1L
                val meeting = mockk<Meeting>()
                val attendance1 = mockk<Attendance>()
                val attendance2 = mockk<Attendance>()
                val response1 = mockk<AttendanceInfoResponse>()
                val response2 = mockk<AttendanceInfoResponse>()

                every { meetingGetService.find(meetingId) } returns meeting
                every {
                    attendanceRepository.findAllByMeetingAndUserStatus(meeting, Status.ACTIVE)
                } returns listOf(attendance1, attendance2)
                every { attendanceMapper.toInfoResponse(attendance1) } returns response1
                every { attendanceMapper.toInfoResponse(attendance2) } returns response2

                val result = queryService.findAllAttendanceByMeeting(meetingId)

                result shouldBe listOf(response1, response2)
            }
        }
    })
