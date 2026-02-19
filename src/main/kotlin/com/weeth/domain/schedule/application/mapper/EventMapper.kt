package com.weeth.domain.schedule.application.mapper

import com.weeth.domain.schedule.application.dto.request.ScheduleSaveRequest
import com.weeth.domain.schedule.application.dto.response.EventResponse
import com.weeth.domain.schedule.domain.entity.Event
import com.weeth.domain.schedule.domain.entity.enums.Type
import com.weeth.domain.user.domain.entity.User
import org.springframework.stereotype.Component

@Component
class EventMapper {
    fun toResponse(event: Event): EventResponse =
        EventResponse(
            id = event.id,
            title = event.title,
            content = event.content,
            location = event.location,
            name = event.user?.name,
            cardinal = event.cardinal,
            type = Type.EVENT,
            start = event.start,
            end = event.end,
            createdAt = event.createdAt,
            modifiedAt = event.modifiedAt,
        )

    fun toEntity(
        request: ScheduleSaveRequest,
        user: User,
    ): Event =
        Event.create(
            title = request.title,
            content = request.content,
            location = request.location,
            cardinal = request.cardinal,
            start = request.start,
            end = request.end,
            user = user,
        )
}
