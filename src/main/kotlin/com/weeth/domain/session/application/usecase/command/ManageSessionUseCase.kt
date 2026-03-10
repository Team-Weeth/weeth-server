package com.weeth.domain.session.application.usecase.command

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.enums.AttendanceStatus
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.club.domain.repository.ClubReader
import com.weeth.domain.schedule.application.dto.request.ScheduleSaveRequest
import com.weeth.domain.schedule.application.dto.request.ScheduleUpdateRequest
import com.weeth.domain.schedule.application.mapper.SessionMapper
import com.weeth.domain.session.application.exception.SessionNotFoundException
import com.weeth.domain.session.domain.repository.SessionRepository
import com.weeth.domain.user.domain.enums.Status
import com.weeth.domain.user.domain.repository.UserReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ManageSessionUseCase(
    private val sessionRepository: SessionRepository,
    private val attendanceRepository: AttendanceRepository,
    private val userReader: UserReader,
    private val cardinalReader: CardinalReader,
    private val sessionMapper: SessionMapper,
    private val clubReader: ClubReader,
) {
    // TODO(PR4): 해당 클럽 소속 admin인지 검증 필요
    @Transactional
    fun create(
        clubId: Long,
        request: ScheduleSaveRequest,
        userId: Long,
    ) {
        val club = clubReader.getClubById(clubId)
        val user = userReader.getById(userId)
        val cardinal = cardinalReader.getByCardinalNumber(request.cardinal)
        // TODO: PR4에서 ClubMember 기반으로 전환 (현재는 user 기반 유지)
        val users = userReader.findAllByCardinalAndStatus(cardinal, Status.ACTIVE)

        val session = sessionMapper.toEntity(club, request, user)
        sessionRepository.save(session)

        attendanceRepository.saveAll(users.map { Attendance.Companion.create(session, it) })
    }

    // TODO(PR4): 해당 클럽 소속 admin인지 검증 필요
    @Transactional
    fun update(
        clubId: Long,
        sessionId: Long,
        request: ScheduleUpdateRequest,
        userId: Long,
    ) {
        val session = sessionRepository.findByIdWithLock(sessionId) ?: throw SessionNotFoundException()
        if (session.club.id != clubId) throw SessionNotFoundException()
        val user = userReader.getById(userId)

        session.updateInfo(request.title, request.content, request.location, request.start, request.end, user)
    }

    // TODO(PR4): 해당 클럽 소속 admin인지 검증 필요
    @Transactional
    fun delete(
        clubId: Long,
        sessionId: Long,
    ) {
        val session = sessionRepository.findByIdWithLock(sessionId) ?: throw SessionNotFoundException()
        if (session.club.id != clubId) throw SessionNotFoundException()
        val attendances = attendanceRepository.findAllBySessionAndUserStatusWithLock(session, Status.ACTIVE)

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
