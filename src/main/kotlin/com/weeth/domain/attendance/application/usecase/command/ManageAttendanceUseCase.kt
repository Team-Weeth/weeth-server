package com.weeth.domain.attendance.application.usecase.command

import com.weeth.domain.attendance.application.dto.request.UpdateAttendanceStatusRequest
import com.weeth.domain.attendance.application.exception.AlreadyAttendedException
import com.weeth.domain.attendance.application.exception.AttendanceCodeMismatchException
import com.weeth.domain.attendance.application.exception.AttendanceNotFoundException
import com.weeth.domain.attendance.application.exception.QrTokenExpiredException
import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.enums.AttendanceStatus
import com.weeth.domain.attendance.domain.port.QrAttendancePort
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.session.application.exception.SessionNotFoundException
import com.weeth.domain.session.application.exception.SessionNotInProgressException
import com.weeth.domain.session.domain.enums.SessionStatus
import com.weeth.domain.session.domain.repository.SessionReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
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

        val storedCode = qrAttendancePort.getCode(sessionId) ?: throw QrTokenExpiredException()
        if (storedCode != code) throw AttendanceCodeMismatchException()

        val session = sessionReader.getById(sessionId)
        if (session.club.id != clubId) throw AttendanceNotFoundException()
        if (!session.isCheckInAllowed(LocalDateTime.now())) throw SessionNotInProgressException()

        val lockedAttendance =
            attendanceRepository.findBySessionAndClubMemberWithLock(session, clubMember)
                ?: throw AttendanceNotFoundException()

        if (lockedAttendance.status == AttendanceStatus.ATTEND) throw AlreadyAttendedException()

        lockedAttendance.attend()
        clubMember.attend()
    }

    @Transactional
    fun close(
        clubId: Long,
        userId: Long,
        now: LocalDate,
        cardinal: Int,
    ) {
        clubMemberPolicy.requireAdmin(clubId, userId)
        val targetSession =
            sessionReader
                .findAllByClubIdAndCardinalIn(clubId, listOf(cardinal))
                .firstOrNull { session ->
                    session.start.toLocalDate().isEqual(now) &&
                        session.end.toLocalDate().isEqual(now)
                }
                ?: throw SessionNotFoundException()

        targetSession.close()
        val attendances =
            attendanceRepository.findAllBySessionAndClubMemberMemberStatus(targetSession, MemberStatus.ACTIVE)
        closePendingAttendances(attendances)
    }

    @Transactional
    fun autoClose() {
        val sessions = sessionReader.findAllByStatusAndEndBeforeOrderByEndAsc(SessionStatus.OPEN, LocalDateTime.now())

        sessions.forEach { session ->
            session.close()
            val attendances =
                attendanceRepository.findAllBySessionAndClubMemberMemberStatus(session, MemberStatus.ACTIVE)
            closePendingAttendances(attendances)
        }
    }

    @Transactional
    fun updateStatus(
        clubId: Long,
        userId: Long,
        attendanceUpdates: List<UpdateAttendanceStatusRequest>,
    ) {
        clubMemberPolicy.requireAdmin(clubId, userId)
        attendanceUpdates.forEach { update ->
            val attendance =
                attendanceRepository.findByIdWithClubMember(update.attendanceId)
                    ?: throw AttendanceNotFoundException()
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
