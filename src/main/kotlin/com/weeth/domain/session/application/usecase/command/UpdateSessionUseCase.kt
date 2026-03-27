package com.weeth.domain.session.application.usecase.command

import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.session.application.dto.request.SessionUpdateRequest
import com.weeth.domain.session.application.exception.ClosedSessionIncludedException
import com.weeth.domain.session.application.exception.EndBeforeStartException
import com.weeth.domain.session.application.exception.SessionErrorCode
import com.weeth.domain.session.application.exception.SessionNotFoundException
import com.weeth.domain.session.domain.entity.Session
import com.weeth.domain.session.domain.enums.SessionStatus
import com.weeth.domain.session.domain.enums.UpdateScope
import com.weeth.domain.session.domain.repository.SessionRepository
import com.weeth.domain.session.domain.service.RecurringSessionPolicy
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.repository.UserReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class UpdateSessionUseCase(
    private val sessionRepository: SessionRepository,
    private val userReader: UserReader,
    private val clubPermissionPolicy: ClubPermissionPolicy,
    private val recurringSessionPolicy: RecurringSessionPolicy,
) {
    @Transactional
    fun update(
        clubId: Long,
        sessionId: Long,
        request: SessionUpdateRequest,
        userId: Long,
        scope: UpdateScope = UpdateScope.THIS_ONLY,
        force: Boolean = false,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)

        val session = sessionRepository.findByIdWithLock(sessionId) ?: throw SessionNotFoundException()
        if (session.club.id != clubId) throw SessionNotFoundException()
        val user = userReader.getById(userId)

        val effectiveStart = request.start ?: session.start
        val effectiveEnd = request.end ?: session.end
        if (effectiveEnd.isBefore(effectiveStart)) throw EndBeforeStartException()

        if (!session.isRecurring || scope == UpdateScope.THIS_ONLY) {
            updateSingleSession(session, request, effectiveStart, effectiveEnd, user)
        } else {
            updateRecurringSessions(session, request, effectiveStart, effectiveEnd, user, force)
        }
    }

    private fun updateSingleSession(
        session: Session,
        request: SessionUpdateRequest,
        effectiveStart: LocalDateTime,
        effectiveEnd: LocalDateTime,
        user: User,
    ) {
        session.updateInfo(
            title = request.title ?: session.title,
            content = request.content ?: session.content,
            location = request.location ?: session.location,
            start = effectiveStart,
            end = effectiveEnd,
            user = user,
        )
    }

    private fun updateRecurringSessions(
        session: Session,
        request: SessionUpdateRequest,
        effectiveStart: LocalDateTime,
        effectiveEnd: LocalDateTime,
        user: User,
        force: Boolean,
    ) {
        val group = checkNotNull(session.sessionGroup) { "반복 세션인데 그룹이 없습니다" }
        val futureSessions =
            sessionRepository.findAllBySessionGroupAndStartGreaterThanEqualWithLock(group, session.start)

        if (!force) {
            val closedCount = futureSessions.count { it.status == SessionStatus.CLOSED }
            if (closedCount > 0) {
                throw ClosedSessionIncludedException(SessionErrorCode.CLOSED_SESSION_INCLUDED_IN_UPDATE, closedCount)
            }
        }

        futureSessions.forEach { s ->
            val (start, end) = recurringSessionPolicy.adjustTime(s.start, effectiveStart, effectiveEnd)
            s.updateInfo(
                title = request.title ?: s.title,
                content = request.content ?: s.content,
                location = request.location ?: s.location,
                start = start,
                end = end,
                user = user,
            )
        }
    }
}
