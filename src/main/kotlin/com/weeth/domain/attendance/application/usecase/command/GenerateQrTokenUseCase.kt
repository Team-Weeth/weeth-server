package com.weeth.domain.attendance.application.usecase.command

import com.weeth.domain.attendance.application.dto.response.QrTokenResponse
import com.weeth.domain.attendance.application.mapper.AttendanceMapper
import com.weeth.domain.attendance.domain.port.QrAttendancePort
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.session.domain.repository.SessionReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class GenerateQrTokenUseCase(
    private val sessionReader: SessionReader,
    private val qrAttendancePort: QrAttendancePort,
    private val attendanceMapper: AttendanceMapper,
    private val clubMemberPolicy: ClubMemberPolicy,
) {
    fun execute(
        sessionId: Long,
        clubId: Long,
        userId: Long,
    ): QrTokenResponse {
        clubMemberPolicy.requireAdmin(clubId, userId)

        val session = sessionReader.getById(sessionId)

        val expiredAt = LocalDateTime.now().plusSeconds(QrAttendancePort.TTL_SECONDS)
        qrAttendancePort.store(sessionId, session.code)

        return attendanceMapper.toQrTokenResponse(session, expiredAt)
    }
}
