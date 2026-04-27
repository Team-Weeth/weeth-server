package com.weeth.domain.attendance.infrastructure

import com.weeth.domain.attendance.application.event.AttendanceSseEvent
import com.weeth.domain.attendance.domain.port.QrAttendancePort
import com.weeth.domain.attendance.domain.port.SseBroadcastPort
import com.weeth.domain.session.domain.repository.SessionReader
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.stereotype.Component

@Component
class QrExpiredEventListener(
    private val sessionReader: SessionReader,
    private val sseBroadcastPort: SseBroadcastPort,
) : MessageListener {
    override fun onMessage(
        message: Message,
        pattern: ByteArray?,
    ) {
        val key = message.body.decodeToString()
        if (!key.startsWith(QrAttendancePort.KEY_PREFIX)) return

        val sessionId = key.removePrefix(QrAttendancePort.KEY_PREFIX).toLongOrNull() ?: return

        runCatching {
            val clubId = sessionReader.findClubIdById(sessionId) ?: return
            sseBroadcastPort.broadcast(clubId, AttendanceSseEvent.QR_CLOSE, null)
        }
    }
}
