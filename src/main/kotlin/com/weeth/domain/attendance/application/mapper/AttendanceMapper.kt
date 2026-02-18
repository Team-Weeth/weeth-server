package com.weeth.domain.attendance.application.mapper

import com.weeth.domain.attendance.application.dto.response.AttendanceDetailResponse
import com.weeth.domain.attendance.application.dto.response.AttendanceInfoResponse
import com.weeth.domain.attendance.application.dto.response.AttendanceResponse
import com.weeth.domain.attendance.application.dto.response.AttendanceSummaryResponse
import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.user.domain.entity.User
import org.springframework.stereotype.Component

@Component
class AttendanceMapper {
    fun toSummaryResponse(
        user: User,
        attendance: Attendance?,
        isAdmin: Boolean = false,
    ): AttendanceSummaryResponse =
        AttendanceSummaryResponse(
            attendanceRate = user.attendanceRate,
            title = attendance?.meeting?.title,
            status = attendance?.status,
            code = if (isAdmin) attendance?.meeting?.code else null,
            start = attendance?.meeting?.start,
            end = attendance?.meeting?.end,
            location = attendance?.meeting?.location,
        )

    fun toDetailResponse(
        user: User,
        attendances: List<AttendanceResponse>,
    ): AttendanceDetailResponse =
        AttendanceDetailResponse(
            attendanceCount = user.attendanceCount ?: 0,
            total = (user.attendanceCount ?: 0) + (user.absenceCount ?: 0),
            absenceCount = user.absenceCount ?: 0,
            attendances = attendances,
        )

    fun toResponse(attendance: Attendance): AttendanceResponse =
        AttendanceResponse(
            id = attendance.id,
            status = attendance.status,
            title = attendance.meeting.title,
            start = attendance.meeting.start,
            end = attendance.meeting.end,
            location = attendance.meeting.location,
        )

    fun toInfoResponse(attendance: Attendance): AttendanceInfoResponse =
        AttendanceInfoResponse(
            id = attendance.id,
            status = attendance.status,
            name = attendance.user.name,
            position = attendance.user.position?.name,
            department = attendance.user.department?.name,
            studentId = attendance.user.studentId,
        )
}
