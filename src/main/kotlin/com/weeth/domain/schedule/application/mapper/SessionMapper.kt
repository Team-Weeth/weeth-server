package com.weeth.domain.schedule.application.mapper

import com.weeth.domain.attendance.domain.entity.Session
import com.weeth.domain.schedule.application.dto.request.ScheduleSaveRequest
import com.weeth.domain.schedule.application.dto.response.SessionInfoResponse
import com.weeth.domain.schedule.application.dto.response.SessionInfosResponse
import com.weeth.domain.schedule.application.dto.response.SessionResponse
import com.weeth.domain.schedule.domain.entity.enums.Type
import com.weeth.domain.user.domain.entity.User
import org.springframework.stereotype.Component

@Component
class SessionMapper {

    fun toResponse(session: Session): SessionResponse =
        SessionResponse(
            id = session.id,
            title = session.title,
            content = session.content,
            location = session.location,
            name = session.user?.name,
            cardinal = session.cardinal,
            type = Type.MEETING,
            code = null,
            start = session.start,
            end = session.end,
            createdAt = session.createdAt,
            modifiedAt = session.modifiedAt,
        )

    fun toAdminResponse(session: Session): SessionResponse =
        SessionResponse(
            id = session.id,
            title = session.title,
            content = session.content,
            location = session.location,
            name = session.user?.name,
            cardinal = session.cardinal,
            type = Type.MEETING,
            code = session.code,
            start = session.start,
            end = session.end,
            createdAt = session.createdAt,
            modifiedAt = session.modifiedAt,
        )

    fun toInfo(session: Session): SessionInfoResponse =
        SessionInfoResponse(
            id = session.id,
            cardinal = session.cardinal,
            title = session.title,
            start = session.start,
        )

    fun toInfos(
        thisWeek: Session?,
        sessions: List<Session>,
    ): SessionInfosResponse =
        SessionInfosResponse(
            thisWeek = thisWeek?.let { toInfo(it) },
            meetings = sessions.map { toInfo(it) },
        )

    fun toEntity(
        request: ScheduleSaveRequest,
        user: User,
    ): Session =
        Session.create(
            title = request.title,
            content = request.content,
            location = request.location,
            cardinal = request.cardinal!!,
            start = request.start!!,
            end = request.end!!,
            user = user,
        )
}
