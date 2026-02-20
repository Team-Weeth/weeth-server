package com.weeth.domain.schedule.application.mapper

import com.weeth.domain.attendance.domain.entity.Session
import com.weeth.domain.schedule.application.dto.response.ScheduleResponse
import com.weeth.domain.schedule.domain.entity.Event
import org.springframework.stereotype.Component

@Component
class ScheduleMapper {
    fun toResponse(
        event: Event,
        isMeeting: Boolean,
    ): ScheduleResponse =
        ScheduleResponse(
            id = event.id,
            title = event.title,
            start = event.start,
            end = event.end,
            isMeeting = isMeeting,
        )

    fun toResponse(
        session: Session,
        isMeeting: Boolean,
    ): ScheduleResponse =
        ScheduleResponse(
            id = session.id,
            title = session.title,
            start = session.start,
            end = session.end,
            isMeeting = isMeeting,
        )
}
