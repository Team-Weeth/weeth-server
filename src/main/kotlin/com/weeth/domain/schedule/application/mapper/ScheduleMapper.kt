package com.weeth.domain.schedule.application.mapper

import com.weeth.domain.schedule.application.dto.response.ScheduleResponse
import com.weeth.domain.schedule.domain.entity.Event
import com.weeth.domain.schedule.domain.enums.Type
import com.weeth.domain.session.domain.entity.Session
import org.springframework.stereotype.Component

@Component
class ScheduleMapper {
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
}
