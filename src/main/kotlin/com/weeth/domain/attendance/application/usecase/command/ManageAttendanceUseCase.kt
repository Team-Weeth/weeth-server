package com.weeth.domain.attendance.application.usecase.command

import com.weeth.domain.attendance.application.dto.request.UpdateAttendanceStatusRequest
import com.weeth.domain.attendance.application.exception.AlreadyAttendedException
import com.weeth.domain.attendance.application.exception.AttendanceNotFoundException
import com.weeth.domain.attendance.application.exception.QrTokenExpiredException
import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.enums.AttendanceStatus
import com.weeth.domain.attendance.domain.port.QrAttendancePort
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.session.application.exception.SessionNotFoundException
import com.weeth.domain.session.domain.enums.SessionStatus
import com.weeth.domain.session.domain.repository.SessionReader
import com.weeth.domain.user.domain.enums.Status
import com.weeth.domain.user.domain.repository.UserReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Todo: 개행을 추가해 가독성 개선
 * Todo: if 문 가독성 개선
 */
@Service
class ManageAttendanceUseCase(
    private val userReader: UserReader,
    private val sessionReader: SessionReader,
    private val attendanceRepository: AttendanceRepository,
    private val qrAttendancePort: QrAttendancePort,
) {
    @Transactional
    fun checkIn(
        userId: Long,
        code: Int,
    ) {
        val sessionId = qrAttendancePort.getSessionId(code) ?: throw QrTokenExpiredException()
        val session = sessionReader.getById(sessionId)
        val user = userReader.getById(userId)
        val lockedAttendance =
            attendanceRepository.findBySessionAndUserWithLock(session, user)
                ?: throw AttendanceNotFoundException()
        if (lockedAttendance.status == AttendanceStatus.ATTEND) throw AlreadyAttendedException()
        lockedAttendance.attend()
        user.attend()
    }

    @Transactional
    fun close(
        now: LocalDate,
        cardinal: Int,
    ) {
        val targetSession =
            sessionReader
                .findAllByCardinalOrderByStartAsc(cardinal)
                .firstOrNull { session ->
                    session.start.toLocalDate().isEqual(now) &&
                        session.end.toLocalDate().isEqual(now)
                }
                ?: throw SessionNotFoundException()
        val attendances = attendanceRepository.findAllBySessionAndUserStatus(targetSession, Status.ACTIVE)
        closePendingAttendances(attendances)
    }

    @Transactional
    fun autoClose() {
        val sessions = sessionReader.findAllByStatusAndEndBeforeOrderByEndAsc(SessionStatus.OPEN, LocalDateTime.now())
        sessions.forEach { session ->
            session.close()
            val attendances = attendanceRepository.findAllBySessionAndUserStatus(session, Status.ACTIVE)
            closePendingAttendances(attendances)
        }
    }

    @Transactional
    fun updateStatus(attendanceUpdates: List<UpdateAttendanceStatusRequest>) {
        attendanceUpdates.forEach { update ->
            val attendance =
                attendanceRepository.findByIdWithUser(update.attendanceId)
                    ?: throw AttendanceNotFoundException()
            val user = attendance.user
            val newStatus = AttendanceStatus.valueOf(update.status)

            if (attendance.status == newStatus) return@forEach

            val prevStatus = attendance.status
            attendance.adminOverride(newStatus)
            if (newStatus == AttendanceStatus.ABSENT) {
                if (prevStatus == AttendanceStatus.ATTEND) user.removeAttend()
                user.absent()
            } else {
                if (prevStatus == AttendanceStatus.ABSENT) user.removeAbsent()
                user.attend()
            }
        }
    }

    private fun closePendingAttendances(attendances: List<Attendance>) {
        attendances
            .filter { it.isPending() }
            .forEach { attendance ->
                attendance.close()
                attendance.user.absent()
            }
    }
}
