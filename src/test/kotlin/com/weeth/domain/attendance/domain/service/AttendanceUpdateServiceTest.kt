package com.weeth.domain.attendance.domain.service

import com.weeth.domain.attendance.domain.entity.enums.AttendanceStatus
import com.weeth.domain.attendance.fixture.AttendanceTestFixture.createActiveUser
import com.weeth.domain.attendance.fixture.AttendanceTestFixture.createAttendance
import com.weeth.domain.attendance.fixture.AttendanceTestFixture.createOneDaySession
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import java.time.LocalDate

class AttendanceUpdateServiceTest :
    DescribeSpec({

        val attendanceUpdateService = AttendanceUpdateService()

        describe("attend") {
            it("attendance.attend() + user.attend()을 호출한다") {
                val session = createOneDaySession(LocalDate.now(), 1, 1234, "1차")
                val userSpy = spyk(createActiveUser("이지훈"))
                every { userSpy.attend() } returns Unit

                val attendanceSpy = spyk(createAttendance(session, userSpy))

                attendanceUpdateService.attend(attendanceSpy)

                verify { attendanceSpy.attend() }
                verify { userSpy.attend() }
            }
        }

        describe("close") {
            it("pending만 close() + user.absent()을 호출한다") {
                val session = createOneDaySession(LocalDate.now(), 1, 1234, "1차")

                val pendingUserSpy = spyk(createActiveUser("pending-user"))
                val nonPendingUserSpy = spyk(createActiveUser("non-pending-user"))
                every { pendingUserSpy.absent() } returns Unit
                every { nonPendingUserSpy.absent() } returns Unit

                val pendingAttendanceSpy = spyk(createAttendance(session, pendingUserSpy))
                val nonPendingAttendanceSpy = spyk(createAttendance(session, nonPendingUserSpy))
                every { pendingAttendanceSpy.isPending() } returns true
                every { nonPendingAttendanceSpy.isPending() } returns false

                attendanceUpdateService.close(listOf(pendingAttendanceSpy, nonPendingAttendanceSpy))

                verify { pendingAttendanceSpy.close() }
                verify { pendingUserSpy.absent() }

                verify(exactly = 0) { nonPendingAttendanceSpy.close() }
                verify(exactly = 0) { nonPendingUserSpy.absent() }
            }
        }

        describe("updateUserAttendanceByStatus") {
            it("ATTEND면 user.removeAttend(), 그 외에는 user.removeAbsent()") {
                val session = createOneDaySession(LocalDate.now(), 1, 1234, "1차")

                val attendUserSpy = spyk(createActiveUser("attend-user"))
                val absentUserSpy = spyk(createActiveUser("absent-user"))
                every { attendUserSpy.removeAttend() } returns Unit
                every { absentUserSpy.removeAbsent() } returns Unit

                val attendAttendanceSpy = spyk(createAttendance(session, attendUserSpy))
                val absentAttendanceSpy = spyk(createAttendance(session, absentUserSpy))
                every { attendAttendanceSpy.status } returns AttendanceStatus.ATTEND
                every { absentAttendanceSpy.status } returns AttendanceStatus.ABSENT
                every { attendAttendanceSpy.user } returns attendUserSpy
                every { absentAttendanceSpy.user } returns absentUserSpy

                attendanceUpdateService.updateUserAttendanceByStatus(
                    listOf(attendAttendanceSpy, absentAttendanceSpy),
                )

                verify { attendUserSpy.removeAttend() }
                verify { absentUserSpy.removeAbsent() }
            }
        }
    })
