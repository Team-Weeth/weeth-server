package com.weeth.domain.attendance.application.usecase.command

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.attendance.fixture.AttendanceTestFixture.createOneDaySession
import com.weeth.domain.schedule.application.exception.MeetingNotFoundException
import com.weeth.domain.schedule.domain.service.MeetingGetService
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.entity.enums.Status
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate

class CloseAttendanceUseCaseTest :
    DescribeSpec({

        val meetingGetService = mockk<MeetingGetService>()
        val attendanceRepository = mockk<AttendanceRepository>()

        val useCase = CloseAttendanceUseCase(meetingGetService, attendanceRepository)

        describe("close") {
            it("당일 정기모임을 찾아 pending 출석을 close") {
                val now = LocalDate.now()
                val targetSession = createOneDaySession(now, 1, 1111, "Today")
                val otherSession = createOneDaySession(now.minusDays(1), 1, 9999, "Yesterday")

                val pendingAttendance = mockk<Attendance>(relaxUnitFun = true)
                val attendedAttendance = mockk<Attendance>(relaxUnitFun = true)
                val pendingUser = mockk<User>(relaxUnitFun = true)

                every { pendingAttendance.isPending() } returns true
                every { pendingAttendance.user } returns pendingUser
                every { attendedAttendance.isPending() } returns false

                every { meetingGetService.find(1) } returns listOf(targetSession, otherSession)
                every {
                    attendanceRepository.findAllBySessionAndUserStatus(targetSession, Status.ACTIVE)
                } returns listOf(pendingAttendance, attendedAttendance)

                useCase.close(now, 1)

                verify { pendingAttendance.close() }
                verify { pendingUser.absent() }
                verify(exactly = 0) { attendedAttendance.close() }
            }

            it("당일 정기모임이 없으면 MeetingNotFoundException") {
                val now = LocalDate.now()
                val otherDaySession = createOneDaySession(now.minusDays(1), 1, 9999, "Yesterday")

                every { meetingGetService.find(1) } returns listOf(otherDaySession)

                shouldThrow<MeetingNotFoundException> {
                    useCase.close(now, 1)
                }
            }
        }
    })
