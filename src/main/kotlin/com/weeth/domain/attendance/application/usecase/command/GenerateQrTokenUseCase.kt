package com.weeth.domain.attendance.application.usecase.command

import com.weeth.domain.attendance.application.dto.response.QrTokenResponse
import com.weeth.domain.attendance.application.event.AttendanceOpenEvent
import com.weeth.domain.attendance.application.mapper.AttendanceMapper
import com.weeth.domain.attendance.domain.port.QrAttendancePort
import com.weeth.domain.attendance.domain.port.SseBroadcastPort
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.session.domain.repository.SessionReader
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class GenerateQrTokenUseCase(
    private val sessionReader: SessionReader,
    private val qrAttendancePort: QrAttendancePort,
    private val attendanceMapper: AttendanceMapper,
    private val clubPermissionPolicy: ClubPermissionPolicy,
    private val ssePort: SseBroadcastPort,
) {
    fun execute(
        sessionId: Long,
        clubId: Long,
        userId: Long,
    ): QrTokenResponse {
        clubPermissionPolicy.requireAdmin(clubId, userId)

        val session = sessionReader.getById(sessionId)

        val expiredAt = LocalDateTime.now().plusSeconds(QrAttendancePort.TTL_SECONDS)
        qrAttendancePort.store(sessionId, session.code)

        val response = attendanceMapper.toQrTokenResponse(session, expiredAt)
        ssePort.broadcast(clubId, EVENT_QR_OPEN, AttendanceOpenEvent(expiredAt))
        return response
    }

    companion object {
        internal const val EVENT_QR_OPEN = "qr-open"
    }
}
