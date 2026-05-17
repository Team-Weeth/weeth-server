package com.weeth.domain.attendance.application.usecase.command

import com.weeth.domain.attendance.application.dto.response.QrTokenResponse
import com.weeth.domain.attendance.application.event.AttendanceOpenEvent
import com.weeth.domain.attendance.application.event.AttendanceSseEvent
import com.weeth.domain.attendance.application.mapper.AttendanceMapper
import com.weeth.domain.attendance.domain.port.QrAttendancePort
import com.weeth.domain.attendance.domain.port.SseBroadcastPort
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.session.application.exception.SessionNotFoundException
import com.weeth.domain.session.domain.repository.SessionReader
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime

@Service
class GenerateQrTokenUseCase(
    private val sessionReader: SessionReader,
    private val qrAttendancePort: QrAttendancePort,
    private val attendanceMapper: AttendanceMapper,
    private val clubPermissionPolicy: ClubPermissionPolicy,
    private val ssePort: SseBroadcastPort,
    transactionManager: PlatformTransactionManager,
) {
    private val txTemplate = TransactionTemplate(transactionManager).apply { isReadOnly = true }

    fun execute(
        sessionId: Long,
        clubId: Long,
        userId: Long,
    ): QrTokenResponse {
        val session =
            requireNotNull(
                txTemplate.execute {
                    clubPermissionPolicy.requireAdmin(clubId, userId)
                    sessionReader.getById(sessionId).also { session ->
                        if (session.club.id != clubId) throw SessionNotFoundException()
                    }
                },
            )

        qrAttendancePort.store(clubId, sessionId, session.code)
        val expiredAt = LocalDateTime.now().plusSeconds(QrAttendancePort.TTL_SECONDS)
        ssePort.broadcast(clubId, AttendanceSseEvent.QR_OPEN, AttendanceOpenEvent(expiredAt))
        return attendanceMapper.toQrTokenResponse(session, expiredAt)
    }
}
