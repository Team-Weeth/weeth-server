package com.weeth.domain.attendance.application.usecase.command

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.entity.enums.AttendanceStatus
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.attendance.domain.repository.SessionRepository
import com.weeth.domain.schedule.application.dto.request.ScheduleSaveRequest
import com.weeth.domain.schedule.application.dto.request.ScheduleUpdateRequest
import com.weeth.domain.schedule.application.dto.response.SessionInfosResponse
import com.weeth.domain.schedule.application.dto.response.SessionResponse
import com.weeth.domain.schedule.application.exception.MeetingNotFoundException
import com.weeth.domain.schedule.application.mapper.SessionMapper
import com.weeth.domain.user.domain.entity.enums.Role
import com.weeth.domain.user.domain.entity.enums.Status
import com.weeth.domain.user.domain.service.CardinalGetService
import com.weeth.domain.user.domain.service.UserGetService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Service
class ManageSessionUseCase(
    private val sessionRepository: SessionRepository,
    private val attendanceRepository: AttendanceRepository,
    private val userGetService: UserGetService,
    private val cardinalGetService: CardinalGetService,
    private val sessionMapper: SessionMapper,
) {
    @Transactional
    fun create(
        request: ScheduleSaveRequest,
        userId: Long,
    ) {
        val user = userGetService.find(userId)
        val cardinal = cardinalGetService.findByUserSide(request.cardinal)
        val users = userGetService.findAllByCardinal(cardinal)
        val session = sessionMapper.toEntity(request, user)
        sessionRepository.save(session)
        attendanceRepository.saveAll(users.map { Attendance.create(session, it) })
    }

    @Transactional
    fun update(
        sessionId: Long,
        request: ScheduleUpdateRequest,
        userId: Long,
    ) {
        val session = sessionRepository.findByIdWithLock(sessionId) ?: throw MeetingNotFoundException()
        val user = userGetService.find(userId)
        session.updateInfo(request.title, request.content, request.location, request.start, request.end, user)
    }

    @Transactional
    fun delete(sessionId: Long) {
        val session = sessionRepository.findByIdWithLock(sessionId) ?: throw MeetingNotFoundException()
        val attendances = attendanceRepository.findAllBySessionAndUserStatus(session, Status.ACTIVE)
        attendances.forEach { a ->
            when (a.status) {
                AttendanceStatus.ATTEND -> a.user.removeAttend()
                AttendanceStatus.ABSENT -> a.user.removeAbsent()
                else -> Unit
            }
        }
        attendanceRepository.deleteAllBySession(session)
        sessionRepository.delete(session)
    }

    fun find(
        userId: Long,
        sessionId: Long,
    ): SessionResponse {
        val user = userGetService.find(userId)
        val session = sessionRepository.findById(sessionId).orElseThrow { MeetingNotFoundException() }
        return if (user.role == Role.ADMIN) {
            sessionMapper.toAdminResponse(session)
        } else {
            sessionMapper.toResponse(session)
        }
    }

    fun findInfos(cardinal: Int?): SessionInfosResponse {
        val sessions =
            if (cardinal == null) {
                sessionRepository.findAllByOrderByStartDesc()
            } else {
                sessionRepository.findAllByCardinalOrderByStartDesc(cardinal)
            }
        val thisWeek = findThisWeek(sessions)
        val sorted = sessions.sortedByDescending { it.start }
        return sessionMapper.toInfos(thisWeek, sorted)
    }

    private fun findThisWeek(
        sessions: List<com.weeth.domain.attendance.domain.entity.Session>,
    ): com.weeth.domain.attendance.domain.entity.Session? {
        val today = LocalDate.now()
        val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        return sessions.firstOrNull { s ->
            val d = s.start.toLocalDate()
            !d.isBefore(startOfWeek) && !d.isAfter(endOfWeek)
        }
    }
}
