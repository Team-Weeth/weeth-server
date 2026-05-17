package com.weeth.domain.attendance.application.usecase.query

import com.weeth.domain.attendance.application.dto.response.QrStatusResponse
import com.weeth.domain.attendance.application.mapper.AttendanceMapper
import com.weeth.domain.attendance.domain.port.QrAttendancePort
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetQrStatusQueryService(
    private val clubMemberPolicy: ClubMemberPolicy,
    private val qrAttendancePort: QrAttendancePort,
    private val attendanceMapper: AttendanceMapper,
) {
    fun findQrStatus(
        clubId: Long,
        userId: Long,
    ): QrStatusResponse {
        clubMemberPolicy.getActiveMember(clubId, userId)

        val sessionId =
            qrAttendancePort.getActiveSessionId(clubId)
                ?: return attendanceMapper.toInactiveQrStatusResponse()
        val expiresAt =
            qrAttendancePort.getExpiredAt(sessionId)
                ?: return attendanceMapper.toInactiveQrStatusResponse()

        return attendanceMapper.toActiveQrStatusResponse(sessionId, expiresAt)
    }
}
