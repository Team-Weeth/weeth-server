package com.weeth.domain.attendance.application.usecase.query

import com.weeth.domain.attendance.domain.entity.Session
import com.weeth.domain.attendance.domain.repository.SessionRepository
import com.weeth.domain.schedule.application.dto.response.SessionInfosResponse
import com.weeth.domain.schedule.application.dto.response.SessionResponse
import com.weeth.domain.schedule.application.exception.SessionNotFoundException
import com.weeth.domain.schedule.application.mapper.SessionMapper
import com.weeth.domain.user.domain.entity.enums.Role
import com.weeth.domain.user.domain.service.UserGetService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Service
@Transactional(readOnly = true)
class GetSessionQueryService(
    private val sessionRepository: SessionRepository,
    private val userGetService: UserGetService,
    private val sessionMapper: SessionMapper,
) {
    fun findSession(
        userId: Long,
        sessionId: Long,
    ): SessionResponse {
        val user = userGetService.find(userId)
        val session = sessionRepository.findByIdOrNull(sessionId) ?: throw SessionNotFoundException()
        return if (user.role == Role.ADMIN) {
            sessionMapper.toAdminResponse(session)
        } else {
            sessionMapper.toResponse(session)
        }
    }

    fun findSessionInfos(cardinal: Int?): SessionInfosResponse {
        val sessions =
            if (cardinal == null) {
                sessionRepository.findAllByOrderByStartDesc()
            } else {
                sessionRepository.findAllByCardinalOrderByStartDesc(cardinal)
            }
        val thisWeek = findThisWeek(sessions)
        return sessionMapper.toInfos(thisWeek, sessions)
    }

    private fun findThisWeek(sessions: List<Session>): Session? {
        val today = LocalDate.now()
        val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        return sessions.firstOrNull { s ->
            val d = s.start.toLocalDate()
            !d.isBefore(startOfWeek) && !d.isAfter(endOfWeek)
        }
    }
}
