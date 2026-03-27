package com.weeth.domain.session.application.mapper

import com.weeth.domain.club.domain.entity.Club
import com.weeth.domain.schedule.application.dto.response.SessionResponse
import com.weeth.domain.schedule.domain.enums.Type
import com.weeth.domain.session.application.dto.request.SessionCreateRequest
import com.weeth.domain.session.application.dto.response.SessionGroupResponse
import com.weeth.domain.session.application.dto.response.SessionInfoResponse
import com.weeth.domain.session.application.dto.response.SessionInfosResponse
import com.weeth.domain.session.domain.entity.Session
import com.weeth.domain.session.domain.entity.SessionGroup
import com.weeth.domain.session.domain.enums.SessionGroupStatus
import com.weeth.domain.session.domain.enums.SessionStatus
import com.weeth.domain.session.domain.service.RecurringSessionPolicy
import com.weeth.domain.user.domain.entity.User
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class SessionMapper(
    private val recurringSessionPolicy: RecurringSessionPolicy,
) {
    fun toResponse(session: Session): SessionResponse =
        SessionResponse(
            id = session.id,
            title = session.title,
            content = session.content,
            location = session.location,
            name = session.user?.name,
            cardinal = session.cardinal,
            type = Type.SESSION,
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
            type = Type.SESSION,
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

    fun toGroupResponse(
        group: SessionGroup,
        sessions: List<Session>,
    ): SessionGroupResponse {
        val completedCount = sessions.count { it.status == SessionStatus.CLOSED }
        val allCompleted = completedCount == sessions.size
        val firstSession = sessions.minByOrNull { it.start }
        return SessionGroupResponse(
            groupId = group.id,
            title = group.title,
            recurrenceType = group.recurrenceType,
            recurrenceDescription =
                recurringSessionPolicy.buildRecurrenceDescription(
                    group.recurrenceType,
                    group.startTime,
                    firstSession?.start?.toLocalDate() ?: group.recurrenceEndDate,
                ),
            startDate = firstSession?.start?.toLocalDate(),
            endDate = group.recurrenceEndDate,
            completedCount = completedCount,
            totalCount = sessions.size,
            status = if (allCompleted) SessionGroupStatus.COMPLETED else SessionGroupStatus.IN_PROGRESS,
            sessions = sessions.sortedBy { it.start }.map { toInfo(it) },
        )
    }

    fun toSingleGroupResponse(session: Session): SessionGroupResponse {
        val completed = session.status == SessionStatus.CLOSED
        return SessionGroupResponse(
            groupId = null,
            title = session.title,
            recurrenceType = null,
            recurrenceDescription = null,
            startDate = session.start.toLocalDate(),
            endDate = null,
            completedCount = if (completed) 1 else 0,
            totalCount = 1,
            status = if (completed) SessionGroupStatus.COMPLETED else SessionGroupStatus.IN_PROGRESS,
            sessions = listOf(toInfo(session)),
        )
    }

    fun toInfos(
        thisWeekSessions: List<Session>,
        groupedSessions: List<SessionGroupResponse>,
    ): SessionInfosResponse =
        SessionInfosResponse(
            thisWeek = thisWeekSessions.map { toInfo(it) },
            sessions = groupedSessions,
        )

    fun toEntity(
        club: Club,
        request: SessionCreateRequest,
        user: User,
    ): Session =
        Session.Companion.create(
            club = club,
            title = request.title,
            content = request.content,
            location = request.location,
            cardinal = request.cardinal,
            start = request.start,
            end = request.end,
            user = user,
        )

    fun toSessionGroup(
        request: SessionCreateRequest,
        recurrenceEndDate: LocalDate,
        totalCount: Int,
    ): SessionGroup =
        SessionGroup(
            title = request.title,
            recurrenceType = checkNotNull(request.recurrenceType),
            recurrenceEndDate = recurrenceEndDate,
            cardinal = request.cardinal,
            startTime = request.start.toLocalTime(),
            endTime = request.end.toLocalTime(),
            totalCount = totalCount,
        )

    fun toEntities(
        club: Club,
        request: SessionCreateRequest,
        user: User,
        sessionGroup: SessionGroup,
        schedules: List<Pair<LocalDateTime, LocalDateTime>>,
    ): List<Session> =
        schedules.map { (start, end) ->
            Session.Companion.create(
                club = club,
                title = request.title,
                content = request.content,
                location = request.location,
                cardinal = request.cardinal,
                start = start,
                end = end,
                user = user,
                sessionGroup = sessionGroup,
            )
        }
}
