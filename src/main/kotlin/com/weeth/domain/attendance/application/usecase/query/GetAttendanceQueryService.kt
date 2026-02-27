package com.weeth.domain.attendance.application.usecase.query

import com.weeth.domain.attendance.application.dto.response.AttendanceDetailResponse
import com.weeth.domain.attendance.application.dto.response.AttendanceInfoResponse
import com.weeth.domain.attendance.application.dto.response.AttendanceSummaryResponse
import com.weeth.domain.attendance.application.exception.SessionNotFoundException
import com.weeth.domain.attendance.application.mapper.AttendanceMapper
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.attendance.domain.repository.SessionRepository
import com.weeth.domain.user.domain.entity.enums.Role
import com.weeth.domain.user.domain.entity.enums.Status
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.domain.user.domain.service.UserCardinalPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class GetAttendanceQueryService(
    private val userReader: UserReader,
    private val userCardinalPolicy: UserCardinalPolicy,
    private val sessionRepository: SessionRepository,
    private val attendanceRepository: AttendanceRepository,
    private val attendanceMapper: AttendanceMapper,
) {
    fun findAttendance(userId: Long): AttendanceSummaryResponse {
        val user = userReader.getById(userId)
        val today = LocalDate.now()

        val todayAttendance =
            attendanceRepository.findTodayByUserId(
                userId,
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay(),
            )

        return attendanceMapper.toSummaryResponse(user, todayAttendance, isAdmin = user.role == Role.ADMIN)
    }

    fun findAllDetailsByCurrentCardinal(userId: Long): AttendanceDetailResponse {
        val user = userReader.getById(userId)
        val currentCardinal = userCardinalPolicy.getCurrentCardinal(user)

        val responses =
            attendanceRepository
                .findAllByUserIdAndCardinal(userId, currentCardinal.cardinalNumber)
                .map(attendanceMapper::toResponse)

        return attendanceMapper.toDetailResponse(user, responses)
    }

    fun findAllAttendanceBySession(sessionId: Long): List<AttendanceInfoResponse> {
        val session = sessionRepository.findById(sessionId).orElseThrow { SessionNotFoundException() }
        val attendances = attendanceRepository.findAllBySessionAndUserStatus(session, Status.ACTIVE)
        return attendances.map(attendanceMapper::toInfoResponse)
    }
}
