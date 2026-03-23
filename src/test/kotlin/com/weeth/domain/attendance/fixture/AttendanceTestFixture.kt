package com.weeth.domain.attendance.fixture

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.session.domain.entity.Session
import com.weeth.domain.user.domain.entity.User
import org.springframework.test.util.ReflectionTestUtils
import java.util.UUID

object AttendanceTestFixture {
    fun createActiveUser(name: String): User =
        User
            .create(
                name = name,
                email = "attendance-${UUID.randomUUID()}@test.com",
                studentId = "",
                tel = "",
                department = "",
            ).also { it.accept() }

    fun createAdminUser(name: String): User = createActiveUser(name)

    fun createAttendance(
        session: Session,
        clubMember: ClubMember,
    ): Attendance = Attendance.create(session, clubMember)

    fun setAttendanceId(
        attendance: Attendance,
        id: Long,
    ) {
        ReflectionTestUtils.setField(attendance, "id", id)
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
