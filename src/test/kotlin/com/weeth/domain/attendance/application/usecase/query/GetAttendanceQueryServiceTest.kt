package com.weeth.domain.attendance.application.usecase.query

import com.weeth.domain.attendance.application.dto.response.AttendanceDetailResponse
import com.weeth.domain.attendance.application.dto.response.AttendanceInfoResponse
import com.weeth.domain.attendance.application.dto.response.AttendanceResponse
import com.weeth.domain.attendance.application.dto.response.AttendanceSummaryResponse
import com.weeth.domain.attendance.application.mapper.AttendanceMapper
import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.attendance.fixture.AttendanceTestFixture.createActiveUser
import com.weeth.domain.schedule.domain.entity.Meeting
import com.weeth.domain.schedule.domain.service.MeetingGetService
import com.weeth.domain.user.domain.entity.Cardinal
import com.weeth.domain.user.domain.entity.enums.Status
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.domain.user.domain.service.UserCardinalPolicy
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class GetAttendanceQueryServiceTest :
    DescribeSpec({

        val userReader = mockk<UserReader>()
        val userCardinalPolicy = mockk<UserCardinalPolicy>()
        val meetingGetService = mockk<MeetingGetService>()
        val attendanceRepository = mockk<AttendanceRepository>()
        val attendanceMapper = mockk<AttendanceMapper>()

        val queryService =
            GetAttendanceQueryService(
                userReader,
                userCardinalPolicy,
                meetingGetService,
                attendanceRepository,
                attendanceMapper,
            )

        val userId = 10L

        describe("find") {
            it("오늘 출석 정보가 있으면 mapper.toSummaryResponse(user, attendance, isAdmin=false) 호출") {
                val user = createActiveUser("이지훈")
                val todayAttendance = mockk<Attendance>()
                val mapped = mockk<AttendanceSummaryResponse>()

                every { userReader.getById(userId) } returns user
                every { attendanceRepository.findTodayByUserId(eq(userId), any(), any()) } returns todayAttendance
                every { attendanceMapper.toSummaryResponse(eq(user), eq(todayAttendance), eq(false)) } returns mapped

                val actual = queryService.findAttendance(userId)

                actual shouldBe mapped
                verify { attendanceMapper.toSummaryResponse(eq(user), eq(todayAttendance), eq(false)) }
            }

            it("오늘 출석이 없다면 mapper.toSummaryResponse(user, null, isAdmin=false) 호출") {
                val user = createActiveUser("이지훈")
                val mapped = mockk<AttendanceSummaryResponse>()

                every { userReader.getById(userId) } returns user
                every { attendanceRepository.findTodayByUserId(eq(userId), any(), any()) } returns null
                every { attendanceMapper.toSummaryResponse(user, null, false) } returns mapped

                val actual = queryService.findAttendance(userId)

                actual shouldBe mapped
                verify { attendanceMapper.toSummaryResponse(user, null, false) }
            }
        }

        describe("findAllDetailsByCurrentCardinal") {
            it("현재 기수의 출석 목록을 매핑하여 Detail 반환") {
                val user = createActiveUser("이지훈")
                val attendance1 = mockk<Attendance>()
                val attendance2 = mockk<Attendance>()

                every { userReader.getById(userId) } returns user
                val currentCardinal = mockk<Cardinal>()
                every { currentCardinal.cardinalNumber } returns 1
                every { userCardinalPolicy.getCurrentCardinal(user) } returns currentCardinal
                every { attendanceRepository.findAllByUserIdAndCardinal(userId, 1) } returns listOf(attendance1, attendance2)

                val response1 = mockk<AttendanceResponse>()
                val response2 = mockk<AttendanceResponse>()
                every { attendanceMapper.toResponse(attendance1) } returns response1
                every { attendanceMapper.toResponse(attendance2) } returns response2

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
