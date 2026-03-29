package com.weeth.domain.session.application.usecase.command

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.enums.AttendanceStatus
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.session.application.exception.ClosedSessionIncludedException
import com.weeth.domain.session.application.exception.SessionErrorCode
import com.weeth.domain.session.application.exception.SessionGroupNotFoundException
import com.weeth.domain.session.application.exception.SessionNotFoundException
import com.weeth.domain.session.domain.entity.Session
import com.weeth.domain.session.domain.entity.SessionGroup
import com.weeth.domain.session.domain.enums.SessionStatus
import com.weeth.domain.session.domain.enums.UpdateScope
import com.weeth.domain.session.domain.repository.SessionGroupRepository
import com.weeth.domain.session.domain.repository.SessionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeleteSessionUseCase(
    private val sessionRepository: SessionRepository,
    private val attendanceRepository: AttendanceRepository,
    private val sessionGroupRepository: SessionGroupRepository,
    private val clubPermissionPolicy: ClubPermissionPolicy,
) {
    @Transactional
    fun delete(
        clubId: Long,
        sessionId: Long,
        userId: Long,
        scope: UpdateScope = UpdateScope.THIS_ONLY,
        force: Boolean = false,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)

        val session = sessionRepository.findByIdWithLock(sessionId) ?: throw SessionNotFoundException()
        if (session.club.id != clubId) throw SessionNotFoundException()

        // 단일 세션인 경우
        if (!session.isRecurring) {
            deleteSingleSession(session)
            return
        }

        val group = checkNotNull(session.sessionGroup) { "반복 세션인데 그룹이 없습니다" }

        // 반복 세션인 경우
        when (scope) {
            UpdateScope.THIS_ONLY -> {
                deleteSingleSession(session)
                val lockedGroup = sessionGroupRepository.findByIdWithLock(group.id) ?: return
                updateOrDeleteGroup(lockedGroup)
            }

            UpdateScope.THIS_AND_FUTURE -> {
                val futureSessions =
                    sessionRepository.findAllBySessionGroupAndStartGreaterThanEqualWithLock(
                        group,
                        session.start,
                    )

                validateNoClosedSessions(futureSessions, force)

                val attendances =
                    attendanceRepository.findAllBySessionInAndClubMemberMemberStatusWithLock(
                        futureSessions,
                        MemberStatus.ACTIVE,
                    )

                rollbackAttendances(attendances)
                attendanceRepository.deleteAllBySessionIn(futureSessions)
                sessionRepository.deleteAll(futureSessions)

                val lockedGroup = sessionGroupRepository.findByIdWithLock(group.id) ?: return
                updateOrDeleteGroup(lockedGroup)
            }
        }
    }

    private fun validateNoClosedSessions(
        futureSessions: List<Session>,
        force: Boolean,
    ) {
        if (!force) {
            val closedCount = futureSessions.count { it.status == SessionStatus.CLOSED }
            if (closedCount > 0) {
                throw ClosedSessionIncludedException(
                    SessionErrorCode.CLOSED_SESSION_INCLUDED_IN_DELETE,
                    closedCount,
                )
            }
        }
    }

    @Transactional
    fun deleteGroup(
        clubId: Long,
        groupId: Long,
        userId: Long,
        force: Boolean = false,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)

        val group =
            sessionGroupRepository.findById(groupId).orElseThrow { SessionGroupNotFoundException() }
        val sessions = sessionRepository.findAllBySessionGroupWithLock(group)

        if (sessions.isEmpty()) {
            sessionGroupRepository.delete(group)
            return
        }

        if (sessions.first().club.id != clubId) throw SessionGroupNotFoundException()

        validateNoClosedSessions(sessions, force)

        val attendances =
            attendanceRepository.findAllBySessionInAndClubMemberMemberStatusWithLock(
                sessions,
                MemberStatus.ACTIVE,
            )

        rollbackAttendances(attendances)
        attendanceRepository.deleteAllBySessionIn(sessions)
        sessionRepository.deleteAll(sessions)
        sessionGroupRepository.delete(group)
    }

    private fun deleteSingleSession(session: Session) {
        val attendances =
            attendanceRepository.findAllBySessionAndClubMemberMemberStatusWithLock(session, MemberStatus.ACTIVE)
        rollbackAttendances(attendances)

        attendanceRepository.deleteAllBySession(session)
        sessionRepository.delete(session)
    }

    private fun rollbackAttendances(attendances: List<Attendance>) {
        attendances.forEach { a ->
            when (a.status) {
                AttendanceStatus.ATTEND -> a.clubMember.removeAttend()
                AttendanceStatus.ABSENT -> a.clubMember.removeAbsent()
                else -> Unit
            }
        }
    }

    private fun updateOrDeleteGroup(group: SessionGroup) {
        val remainingCount = sessionRepository.countBySessionGroup(group)

        if (remainingCount == 0L) {
            sessionGroupRepository.delete(group)
        } else {
            group.updateTotalCount(remainingCount.toInt())
        }
    }
}
