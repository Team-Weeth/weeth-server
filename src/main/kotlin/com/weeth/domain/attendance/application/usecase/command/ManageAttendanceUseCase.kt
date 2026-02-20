package com.weeth.domain.attendance.application.usecase.command

import com.weeth.domain.attendance.application.dto.request.UpdateAttendanceStatusRequest
import com.weeth.domain.attendance.application.exception.AttendanceCodeMismatchException
import com.weeth.domain.attendance.application.exception.AttendanceNotFoundException
import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.entity.enums.AttendanceStatus
import com.weeth.domain.attendance.domain.entity.enums.SessionStatus
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.attendance.domain.repository.SessionRepository
import com.weeth.domain.schedule.application.exception.MeetingNotFoundException
import com.weeth.domain.user.domain.entity.enums.Status
import com.weeth.domain.user.domain.service.UserGetService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class ManageAttendanceUseCase(
    private val userGetService: UserGetService,
    private val sessionRepository: SessionRepository,
    private val attendanceRepository: AttendanceRepository,
) {
    @Transactional
    fun checkIn(
        userId: Long,
        code: Int,
    ) {
        val user = userGetService.find(userId)
        val now = LocalDateTime.now()
        val todayAttendance =
            attendanceRepository.findCurrentByUserId(userId, now, now.plusMinutes(10))
                ?: throw AttendanceNotFoundException()
        if (todayAttendance.isWrong(code)) {
            throw AttendanceCodeMismatchException()
        }
        val lockedAttendance =
            attendanceRepository.findBySessionAndUserWithLock(todayAttendance.session, user)
                ?: throw AttendanceNotFoundException()
        if (lockedAttendance.status != AttendanceStatus.ATTEND) {
            lockedAttendance.attend()
            user.attend()
        }
    }

    @Transactional
    fun close(
        now: LocalDate,
        cardinal: Int,
    ) {
        val targetSession =
            sessionRepository
                .findAllByCardinalOrderByStartAsc(cardinal)
                .firstOrNull { session -> session.start.toLocalDate().isEqual(now) && session.end.toLocalDate().isEqual(now) }
                ?: throw MeetingNotFoundException()
        val attendances = attendanceRepository.findAllBySessionAndUserStatus(targetSession, Status.ACTIVE)
        closePendingAttendances(attendances)
    }

    @Transactional
    fun autoClose() {
        val sessions = sessionRepository.findAllByStatusAndEndBeforeOrderByEndAsc(SessionStatus.OPEN, LocalDateTime.now())
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
            if (newStatus == AttendanceStatus.ABSENT) {
                attendance.close()
                user.removeAttend()
                user.absent()
            } else {
                attendance.attend()
                user.removeAbsent()
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
