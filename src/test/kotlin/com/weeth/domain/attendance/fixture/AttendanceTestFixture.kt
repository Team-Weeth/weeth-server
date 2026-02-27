package com.weeth.domain.attendance.fixture

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.entity.Session
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.entity.enums.Role
import com.weeth.domain.user.domain.entity.enums.Status
import com.weeth.domain.user.domain.vo.AttendanceStats
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate
import java.time.LocalDateTime

object AttendanceTestFixture {
    fun createActiveUser(name: String): User =
        User(
            name = name,
            status = Status.ACTIVE,
        )

    fun createAdminUser(name: String): User =
        User(
            name = name,
            status = Status.ACTIVE,
            role = Role.ADMIN,
        )

    fun createAttendance(
        session: Session,
        user: User,
    ): Attendance = Attendance.create(session, user)

    fun createOneDaySession(
        date: LocalDate,
        cardinal: Int,
        code: Int,
        title: String,
    ): Session =
        Session(
            title = title,
            location = "Test Location",
            start = date.atTime(10, 0),
            end = date.atTime(12, 0),
            code = code,
            cardinal = cardinal,
        )

    fun createInProgressSession(
        cardinal: Int,
        code: Int,
        title: String,
    ): Session =
        Session(
            title = title,
            location = "Test Location",
            start = LocalDateTime.now().minusMinutes(5),
            end = LocalDateTime.now().plusMinutes(5),
            code = code,
            cardinal = cardinal,
        )

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
        ReflectionTestUtils.setField(
            user,
            "attendanceStats",
            AttendanceStats(
                attendanceCount = attendanceCount,
                absenceCount = absenceCount,
                attendanceRate = if (attendanceCount + absenceCount > 0) (attendanceCount * 100) / (attendanceCount + absenceCount) else 0,
            ),
        )
    }

    fun enrichUserProfile(
        user: User,
        department: String,
        studentId: String,
    ) {
        ReflectionTestUtils.setField(user, "department", department)
        ReflectionTestUtils.setField(user, "studentId", studentId)
    }
}
