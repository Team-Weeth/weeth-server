package com.weeth.domain.attendance.application.usecase.query

import com.weeth.domain.attendance.application.dto.response.AttendanceDetailResponse
import com.weeth.domain.attendance.application.dto.response.AttendanceInfoResponse
import com.weeth.domain.attendance.application.dto.response.AttendanceSummaryResponse
import com.weeth.domain.attendance.application.mapper.AttendanceMapper
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.session.domain.repository.SessionReader
import com.weeth.domain.user.domain.enums.Role
import com.weeth.domain.user.domain.enums.Status
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
    private val sessionReader: SessionReader,
    private val attendanceRepository: AttendanceRepository,
    private val attendanceMapper: AttendanceMapper,
) {
    // TODO: PR4에서 clubMember 기반으로 전환 (현재는 user 기반 유지)
    fun findAttendance(
        clubId: Long,
        userId: Long,
    ): AttendanceSummaryResponse {
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

    // TODO: PR4에서 clubMember 기반으로 전환 (현재는 user 기반 유지)
    fun findAllDetailsByCurrentCardinal(
        clubId: Long,
        userId: Long,
    ): AttendanceDetailResponse {
        val user = userReader.getById(userId)
        val currentCardinal = userCardinalPolicy.getCurrentCardinal(user)

        val responses =
            attendanceRepository
                .findAllByUserIdAndCardinal(userId, currentCardinal.cardinalNumber)
                .map(attendanceMapper::toResponse)

        return attendanceMapper.toDetailResponse(user, responses)
    }

    // TODO: PR4에서 clubMember 기반으로 전환 (현재는 user 기반 유지)
    fun findAllAttendanceBySession(
        clubId: Long,
        sessionId: Long,
    ): List<AttendanceInfoResponse> {
        val session = sessionReader.getById(sessionId)

        val attendances = attendanceRepository.findAllBySessionAndUserStatus(session, Status.ACTIVE)
        return attendances.map(attendanceMapper::toInfoResponse)
    }
}
