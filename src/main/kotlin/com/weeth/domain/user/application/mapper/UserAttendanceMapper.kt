package com.weeth.domain.user.application.mapper

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.user.application.dto.response.UserAttendedSessionResponse
import com.weeth.global.common.id.TsidBase62Encoder
import org.springframework.stereotype.Component

@Component
class UserAttendanceMapper {
    fun toAttendedSessionResponse(attendance: Attendance): UserAttendedSessionResponse {
        val session = attendance.session
        val club = session.club
        return UserAttendedSessionResponse(
            attendanceId = attendance.id,
            clubId = TsidBase62Encoder.encode(club.id),
            clubName = club.name,
            sessionId = session.id,
            sessionTitle = session.title,
            cardinal = session.cardinal,
            start = session.start,
            end = session.end,
            status = attendance.status,
        )
    }
}
