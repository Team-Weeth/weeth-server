package com.weeth.domain.attendance.application.mapper

import com.weeth.domain.attendance.application.dto.response.AttendanceDetailResponse
import com.weeth.domain.attendance.application.dto.response.AttendanceInfoResponse
import com.weeth.domain.attendance.application.dto.response.AttendanceResponse
import com.weeth.domain.attendance.application.dto.response.AttendanceSummaryResponse
import com.weeth.domain.attendance.application.dto.response.QrTokenResponse
import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.enums.AttendanceStatus
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.vo.ClubAttendanceStats
import com.weeth.domain.session.domain.entity.Session
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class AttendanceMapper {
    fun toSummaryResponse(
        clubMember: ClubMember,
        attendance: Attendance?,
    ): AttendanceSummaryResponse =
        AttendanceSummaryResponse(
            attendanceRate = clubMember.attendanceStats.attendanceRate,
            title = attendance?.session?.title,
            status = attendance?.status,
            sessionId = attendance?.session?.id,
            start = attendance?.session?.start,
            end = attendance?.session?.end,
            location = attendance?.session?.location,
        )

    fun toDetailResponse(
        cardinalNumber: Int,
        attendances: List<Attendance>,
    ): AttendanceDetailResponse {
        val stats = toStats(attendances)
        return AttendanceDetailResponse(
            attendanceCount = stats.attendanceCount,
            total = stats.attendanceCount + stats.absenceCount,
            absenceCount = stats.absenceCount,
            attendances = attendances.map(::toResponse),
            cardinalNumber = cardinalNumber,
            attendanceRate = stats.attendanceRate,
        )
    }

    fun toStats(attendances: List<Attendance>): ClubAttendanceStats =
        ClubAttendanceStats.fromCounts(
            attendances.count { it.status == AttendanceStatus.ATTEND },
            attendances.count { it.status == AttendanceStatus.ABSENT },
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
            memberStatus = attendance.clubMember.memberStatus,
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
