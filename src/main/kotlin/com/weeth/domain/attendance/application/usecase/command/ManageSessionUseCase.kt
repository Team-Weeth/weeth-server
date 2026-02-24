package com.weeth.domain.attendance.application.usecase.command

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.entity.enums.AttendanceStatus
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.attendance.domain.repository.SessionRepository
import com.weeth.domain.schedule.application.dto.request.ScheduleSaveRequest
import com.weeth.domain.schedule.application.dto.request.ScheduleUpdateRequest
import com.weeth.domain.schedule.application.exception.SessionNotFoundException
import com.weeth.domain.schedule.application.mapper.SessionMapper
import com.weeth.domain.user.domain.entity.enums.Status
import com.weeth.domain.user.domain.service.CardinalGetService
import com.weeth.domain.user.domain.service.UserGetService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
        val session = sessionRepository.findByIdWithLock(sessionId) ?: throw SessionNotFoundException()
        val user = userGetService.find(userId)
        session.updateInfo(request.title, request.content, request.location, request.start, request.end, user)
    }

    @Transactional
    fun delete(sessionId: Long) {
        val session = sessionRepository.findByIdWithLock(sessionId) ?: throw SessionNotFoundException()
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
}
