package com.weeth.domain.attendance.application.usecase.command

import com.weeth.domain.attendance.application.dto.request.UpdateAttendanceStatusRequest
import com.weeth.domain.attendance.application.exception.AlreadyAttendedException
import com.weeth.domain.attendance.application.exception.AttendanceAlreadyClosedException
import com.weeth.domain.attendance.application.exception.AttendanceCodeMismatchException
import com.weeth.domain.attendance.application.exception.AttendanceNotFoundException
import com.weeth.domain.attendance.application.exception.QrTokenExpiredException
import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.enums.AttendanceStatus
import com.weeth.domain.attendance.domain.port.QrAttendancePort
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.session.application.exception.SessionNotInProgressException
import com.weeth.domain.session.domain.entity.Session
import com.weeth.domain.session.domain.enums.SessionStatus
import com.weeth.domain.session.domain.repository.SessionReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ManageAttendanceUseCase(
    private val clubMemberPolicy: ClubMemberPolicy,
    private val sessionReader: SessionReader,
    private val attendanceRepository: AttendanceRepository,
    private val qrAttendancePort: QrAttendancePort,
) {
    @Transactional
    fun checkIn(
        clubId: Long,
        userId: Long,
        sessionId: Long,
        code: Int,
    ) {
        val clubMember = clubMemberPolicy.getActiveMember(clubId, userId)

        val session = sessionReader.getById(sessionId)
        if (session.club.id != clubId) throw AttendanceNotFoundException()
        if (!session.isCheckInAllowed(LocalDateTime.now())) throw SessionNotInProgressException()

        val storedCode = qrAttendancePort.getCode(sessionId) ?: throw QrTokenExpiredException()
        if (storedCode != code) throw AttendanceCodeMismatchException()

        val lockedAttendance =
            attendanceRepository.findBySessionAndClubMemberWithLock(session, clubMember)
                ?: throw AttendanceNotFoundException()

        when (lockedAttendance.status) {
            AttendanceStatus.ATTEND -> throw AlreadyAttendedException()
            AttendanceStatus.ABSENT -> throw AttendanceAlreadyClosedException()
            AttendanceStatus.PENDING -> Unit
        }

        lockedAttendance.attend()
        clubMember.attend()
    }

    @Transactional
    fun autoClose() {
        val sessions = sessionReader.findAllByStatusAndEndBeforeOrderByEndAsc(SessionStatus.OPEN, LocalDateTime.now())
        sessions.forEach { session -> closeSingleSession(session) }
    }

    private fun closeSingleSession(session: Session) {
        session.close()
        val attendances =
            attendanceRepository.findAllBySessionAndClubMemberMemberStatusWithLock(session, MemberStatus.ACTIVE)
        closePendingAttendances(attendances)
    }

    @Transactional
    fun updateStatus(
        clubId: Long,
        userId: Long,
        attendanceUpdates: List<UpdateAttendanceStatusRequest>,
    ) {
        clubMemberPolicy.requireAdmin(clubId, userId)
        if (attendanceUpdates.isEmpty()) return
        // 데드락 방지: 일관된 순서로 락 획득
        val ids = attendanceUpdates.map { it.attendanceId }.sorted()
        val attendanceMap = attendanceRepository.findAllByIdsWithLock(ids).associateBy { it.id }
        // 데드락 방지: 처리 순서도 ID 오름차순으로 통일
        attendanceUpdates.sortedBy { it.attendanceId }.forEach { update ->
            val attendance = attendanceMap[update.attendanceId] ?: throw AttendanceNotFoundException()
            if (attendance.clubMember.club.id != clubId) throw AttendanceNotFoundException()

            val member = attendance.clubMember
            val newStatus = AttendanceStatus.valueOf(update.status)
            if (attendance.status == newStatus) return@forEach

            val prevStatus = attendance.status
            attendance.adminOverride(newStatus)

            when (newStatus) {
                AttendanceStatus.ABSENT -> {
                    if (prevStatus == AttendanceStatus.ATTEND) member.removeAttend()
                    member.absent()
                }

                AttendanceStatus.ATTEND -> {
                    if (prevStatus == AttendanceStatus.ABSENT) member.removeAbsent()
                    member.attend()
                }

                AttendanceStatus.PENDING -> {
                    if (prevStatus == AttendanceStatus.ATTEND) member.removeAttend()
                    if (prevStatus == AttendanceStatus.ABSENT) member.removeAbsent()
                }
            }
        }
    }

    private fun closePendingAttendances(attendances: List<Attendance>) {
        attendances
            .filter { it.isPending() }
            .forEach { attendance ->
                attendance.close()
                attendance.clubMember.absent()
            }
    }
}
