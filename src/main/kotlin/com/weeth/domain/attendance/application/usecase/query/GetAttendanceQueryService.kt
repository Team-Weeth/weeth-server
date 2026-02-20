package com.weeth.domain.attendance.application.usecase.query

import com.weeth.domain.attendance.application.dto.response.AttendanceDetailResponse
import com.weeth.domain.attendance.application.dto.response.AttendanceInfoResponse
import com.weeth.domain.attendance.application.dto.response.AttendanceSummaryResponse
import com.weeth.domain.attendance.application.mapper.AttendanceMapper
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.attendance.domain.repository.SessionRepository
import com.weeth.domain.schedule.application.exception.MeetingNotFoundException
import com.weeth.domain.user.domain.entity.enums.Role
import com.weeth.domain.user.domain.entity.enums.Status
import com.weeth.domain.user.domain.service.UserCardinalGetService
import com.weeth.domain.user.domain.service.UserGetService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class GetAttendanceQueryService(
    private val userGetService: UserGetService,
    private val userCardinalGetService: UserCardinalGetService,
    private val sessionRepository: SessionRepository,
    private val attendanceRepository: AttendanceRepository,
    private val mapper: AttendanceMapper,
) {
    fun findAttendance(userId: Long): AttendanceSummaryResponse {
        val user = userGetService.find(userId)
        val today = LocalDate.now()

        val todayAttendance =
            attendanceRepository.findTodayByUserId(
                userId,
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay(),
            )

        return mapper.toSummaryResponse(user, todayAttendance, isAdmin = user.role == Role.ADMIN)
    }

    fun findAllDetailsByCurrentCardinal(userId: Long): AttendanceDetailResponse {
        val user = userGetService.find(userId)
        val currentCardinal = userCardinalGetService.getCurrentCardinal(user)

        val responses =
            attendanceRepository
                .findAllByUserIdAndCardinal(userId, currentCardinal.cardinalNumber)
                .map(mapper::toResponse)

        return mapper.toDetailResponse(user, responses)
    }

    fun findAllAttendanceBySession(sessionId: Long): List<AttendanceInfoResponse> {
        val session = sessionRepository.findById(sessionId).orElseThrow { MeetingNotFoundException() }
        val attendances = attendanceRepository.findAllBySessionAndUserStatus(session, Status.ACTIVE)
        return attendances.map(mapper::toInfoResponse)
    }
}
