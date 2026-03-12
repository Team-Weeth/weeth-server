package com.weeth.domain.attendance.application.mapper

import com.weeth.domain.attendance.application.dto.response.AttendanceDetailResponse
import com.weeth.domain.attendance.application.dto.response.AttendanceInfoResponse
import com.weeth.domain.attendance.application.dto.response.AttendanceResponse
import com.weeth.domain.attendance.application.dto.response.AttendanceSummaryResponse
import com.weeth.domain.attendance.application.dto.response.QrTokenResponse
import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.session.domain.entity.Session
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class AttendanceMapper {
    fun toSummaryResponse(
        clubMember: ClubMember,
        attendance: Attendance?,
        isAdmin: Boolean = false,
    ): AttendanceSummaryResponse =
        AttendanceSummaryResponse(
            attendanceRate = clubMember.attendanceStats.attendanceRate,
            title = attendance?.session?.title,
            status = attendance?.status,
            code = if (isAdmin) attendance?.session?.code else null,
            start = attendance?.session?.start,
            end = attendance?.session?.end,
            location = attendance?.session?.location,
        )

    fun toDetailResponse(
        clubMember: ClubMember,
        attendances: List<AttendanceResponse>,
    ): AttendanceDetailResponse =
        AttendanceDetailResponse(
            attendanceCount = clubMember.attendanceStats.attendanceCount,
            total = clubMember.attendanceStats.attendanceCount + clubMember.attendanceStats.absenceCount,
            absenceCount = clubMember.attendanceStats.absenceCount,
            attendances = attendances,
        )

    fun toResponse(attendance: Attendance): AttendanceResponse =
        AttendanceResponse(
            id = attendance.id,
            status = attendance.status,
            title = attendance.session.title,
            start = attendance.session.start,
            end = attendance.session.end,
            location = attendance.session.location,
        )

    fun toInfoResponse(attendance: Attendance): AttendanceInfoResponse =
        AttendanceInfoResponse(
            id = attendance.id,
            status = attendance.status,
            name = attendance.clubMember.user.name,
            department = attendance.clubMember.user.department,
            studentId = attendance.clubMember.user.studentId,
        )

    fun toQrTokenResponse(
        session: Session,
        expiredAt: LocalDateTime,
    ): QrTokenResponse =
        QrTokenResponse(
            sessionId = session.id,
            code = session.code,
            expiredAt = expiredAt,
        )
}
