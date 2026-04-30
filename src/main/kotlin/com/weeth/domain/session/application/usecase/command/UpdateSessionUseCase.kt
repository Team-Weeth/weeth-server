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

    /**
     * 반복 세션을 수정한다.
     * 반복 수정을 하는 경우 세션/출석의 상태를 유지하기 위해 별도로 세션을 삭제/재생성 하지 않고, in-place로 갱신한다.
     * 이 경우 반복 세션 중 특정 세션 이후의 시간을 미루는 경우 이전 날짜의 세션이 남아있을 수 있으나, 이는 사용자가 삭제할 수 있도록 유지한다.
     */
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

        val effectiveTitle = request.title ?: session.title

        futureSessions.forEach { s ->
            val (start, end) = recurringSessionPolicy.adjustTime(s.start, effectiveStart, effectiveEnd)
            s.updateInfo(
                title = effectiveTitle,
                content = request.content ?: s.content,
                location = request.location ?: s.location,
                start = start,
                end = end,
                user = user,
            )
        }

        group.updateMetadata(
            title = effectiveTitle,
            startTime = effectiveStart.toLocalTime(),
            endTime = effectiveEnd.toLocalTime(),
        )
    }
}
