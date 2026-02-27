package com.weeth.domain.schedule.application.mapper

import com.weeth.domain.schedule.application.dto.response.ScheduleResponse
import com.weeth.domain.schedule.domain.entity.Event
import com.weeth.domain.session.domain.entity.Session
import org.springframework.stereotype.Component

@Component
class ScheduleMapper {
    fun toResponse(
        event: Event,
        isSession: Boolean,
    ): ScheduleResponse =
        ScheduleResponse(
            id = event.id,
            title = event.title,
            start = event.start,
            end = event.end,
            isSession = isSession,
        )

    fun toResponse(
        session: Session,
        isSession: Boolean,
    ): ScheduleResponse =
        ScheduleResponse(
            id = session.id,
            title = session.title,
            start = session.start,
            end = session.end,
            isSession = isSession,
        )
}
