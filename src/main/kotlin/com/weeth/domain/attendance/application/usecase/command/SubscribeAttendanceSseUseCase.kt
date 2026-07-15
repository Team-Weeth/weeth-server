package com.weeth.domain.attendance.application.usecase.command

import com.weeth.domain.attendance.application.event.AttendanceOpenEvent
import com.weeth.domain.attendance.application.event.AttendanceSseEvent
import com.weeth.domain.attendance.domain.port.QrAttendancePort
import com.weeth.domain.attendance.domain.port.SseBroadcastPort
import com.weeth.domain.attendance.domain.port.SseSubscribePort
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Service
class SubscribeAttendanceSseUseCase(
    private val sseSubscribePort: SseSubscribePort,
    private val sseBroadcastPort: SseBroadcastPort,
    private val clubMemberPolicy: ClubMemberPolicy,
    private val qrAttendancePort: QrAttendancePort,
) {
    @Transactional(readOnly = true)
    fun execute(
        clubId: Long,
        userId: Long,
    ): SseEmitter {
        clubMemberPolicy.getActiveMember(clubId, userId)

        val emitter = sseSubscribePort.subscribe(clubId, userId)

        val activeSessionId = qrAttendancePort.getActiveSessionId(clubId)
        val expiredAt = activeSessionId?.let { qrAttendancePort.getExpiredAt(it) }

        if (expiredAt != null) {
            sseBroadcastPort.sendToUser(clubId, userId, AttendanceSseEvent.QR_OPEN, AttendanceOpenEvent(expiredAt))
        } else {
            sseBroadcastPort.sendToUser(clubId, userId, AttendanceSseEvent.QR_NONE, null)
        }

        return emitter
    }
}
