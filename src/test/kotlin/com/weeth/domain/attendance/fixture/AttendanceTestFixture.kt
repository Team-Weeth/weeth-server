package com.weeth.domain.attendance.fixture

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.schedule.domain.entity.Meeting
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.entity.enums.Department
import com.weeth.domain.user.domain.entity.enums.Position
import com.weeth.domain.user.domain.entity.enums.Role
import com.weeth.domain.user.domain.entity.enums.Status
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate
import java.time.LocalDateTime

object AttendanceTestFixture {
    fun createActiveUser(name: String): User =
        User
            .builder()
            .name(name)
            .status(Status.ACTIVE)
            .build()

    fun createAdminUser(name: String): User =
        User
            .builder()
            .name(name)
            .status(Status.ACTIVE)
            .role(Role.ADMIN)
            .build()

    fun createActiveUserWithAttendances(
        name: String,
        meetings: List<Meeting>,
    ): User {
        val user = createActiveUser(name)
        initAttendancesField(user)
        meetings.forEach { meeting ->
            val attendance = createAttendance(meeting, user)
            user.add(attendance)
        }
        return user
    }

    fun createAdminUserWithAttendances(
        name: String,
        meetings: List<Meeting>,
    ): User {
        val user = createAdminUser(name)
        initAttendancesField(user)
        meetings.forEach { meeting ->
            val attendance = createAttendance(meeting, user)
            user.add(attendance)
        }
        return user
    }

    fun createAttendance(
        meeting: Meeting,
        user: User,
    ): Attendance = Attendance(meeting, user)

    fun createOneDayMeeting(
        date: LocalDate,
        cardinal: Int,
        code: Int,
        title: String,
    ): Meeting =
        Meeting
            .builder()
            .title(title)
            .location("Test Location")
            .start(date.atTime(10, 0))
            .end(date.atTime(12, 0))
            .code(code)
            .cardinal(cardinal)
            .build()

    fun createInProgressMeeting(
        cardinal: Int,
        code: Int,
        title: String,
    ): Meeting =
        Meeting
            .builder()
            .title(title)
            .location("Test Location")
            .start(LocalDateTime.now().minusMinutes(5))
            .end(LocalDateTime.now().plusMinutes(5))
            .code(code)
            .cardinal(cardinal)
            .build()

    fun setAttendanceId(
        attendance: Attendance,
        id: Long,
    ) {
        ReflectionTestUtils.setField(attendance, "id", id)
    }

    fun setUserAttendanceStats(
        user: User,
        attendanceCount: Int,
        absenceCount: Int,
    ) {
        ReflectionTestUtils.setField(user, "attendanceCount", attendanceCount)
        ReflectionTestUtils.setField(user, "absenceCount", absenceCount)
    }

    fun enrichUserProfile(
        user: User,
        position: Position,
        department: Department,
        studentId: String,
    ) {
        ReflectionTestUtils.setField(user, "position", position)
        ReflectionTestUtils.setField(user, "department", department)
        ReflectionTestUtils.setField(user, "studentId", studentId)
    }

    fun enrichUserProfile(
        user: User,
        position: Position,
        departmentKoreanValue: String,
        studentId: String,
    ) {
        ReflectionTestUtils.setField(user, "position", position)
        val department = Department.to(departmentKoreanValue)
        ReflectionTestUtils.setField(user, "department", department)
        ReflectionTestUtils.setField(user, "studentId", studentId)
    }

    private fun initAttendancesField(user: User) {
        if (user.attendances == null) {
            ReflectionTestUtils.setField(user, "attendances", mutableListOf<Attendance>())
        }
    }
}
