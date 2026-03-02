package com.weeth.domain.attendance.application.usecase.command

import com.weeth.domain.attendance.application.dto.response.QrTokenResponse
import com.weeth.domain.attendance.application.mapper.AttendanceMapper
import com.weeth.domain.attendance.domain.port.QrAttendancePort
import com.weeth.domain.session.domain.repository.SessionReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class GenerateQrTokenUseCase(
    private val sessionReader: SessionReader,
    private val qrAttendancePort: QrAttendancePort,
    private val attendanceMapper: AttendanceMapper,
) {
    @Transactional
    fun execute(sessionId: Long): QrTokenResponse {
        val session = sessionReader.getById(sessionId)
        val expiredAt = LocalDateTime.now().plusSeconds(TTL_SECONDS)
        qrAttendancePort.store(session.code, sessionId)
        return attendanceMapper.toQrTokenResponse(session, expiredAt)
    }

    companion object {
        private const val TTL_SECONDS = 600L
    }
}
