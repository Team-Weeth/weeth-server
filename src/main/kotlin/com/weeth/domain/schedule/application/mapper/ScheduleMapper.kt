package com.weeth.domain.schedule.application.mapper

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.schedule.application.dto.response.AttendeeResponse
import com.weeth.domain.schedule.application.dto.response.ScheduleAttendanceStatus
import com.weeth.domain.schedule.application.dto.response.ScheduleDetailResponse
import com.weeth.domain.schedule.application.dto.response.ScheduleResponse
import com.weeth.domain.schedule.domain.entity.Event
import com.weeth.domain.schedule.domain.enums.Type
import com.weeth.domain.session.domain.entity.Session
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ScheduleMapper(
    private val fileAccessUrlPort: FileAccessUrlPort,
) {
    fun toResponse(event: Event): ScheduleResponse =
        ScheduleResponse(
            id = event.id,
            title = event.title,
            start = event.start,
            end = event.end,
            type = Type.EVENT,
            location = event.location,
            cardinal = event.cardinal,
        )

    fun toResponse(session: Session): ScheduleResponse =
        ScheduleResponse(
            id = session.id,
            title = session.title,
            start = session.start,
            end = session.end,
            type = Type.SESSION,
            location = session.location,
            cardinal = session.cardinal,
        )

    fun toDetailResponse(event: Event): ScheduleDetailResponse =
        ScheduleDetailResponse(
            id = event.id,
            type = Type.EVENT,
            title = event.title,
            description = event.content,
            location = event.location,
            start = event.start,
            end = event.end,
            creatorName = event.user?.name,
            myAttendanceStatus = null,
            attendedAt = null,
            totalAttendees = null,
            attendees = null,
        )

    fun toDetailResponse(
        session: Session,
        attendances: List<Attendance>,
        myAttendanceStatus: ScheduleAttendanceStatus,
        attendedAt: LocalDateTime?,
    ): ScheduleDetailResponse =
        ScheduleDetailResponse(
            id = session.id,
            type = Type.SESSION,
            title = session.title,
            description = session.content,
            location = session.location,
            start = session.start,
            end = session.end,
            creatorName = session.user?.name,
            myAttendanceStatus = myAttendanceStatus,
            attendedAt = attendedAt,
            totalAttendees = attendances.size,
            attendees = attendances.map { toAttendeeResponse(it) },
        )

    private fun toAttendeeResponse(attendance: Attendance): AttendeeResponse =
        AttendeeResponse(
            name = attendance.clubMember.user.name,
            department = attendance.clubMember.user.department,
            role = attendance.clubMember.memberRole,
            profileImageUrl = attendance.clubMember.profileImageStorageKey?.let { fileAccessUrlPort.resolve(it) },
        )
}
