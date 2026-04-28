package com.weeth.domain.attendance.application.usecase.command

import com.weeth.domain.attendance.application.event.AttendanceOpenEvent
import com.weeth.domain.attendance.application.event.AttendanceSseEvent
import com.weeth.domain.attendance.domain.port.QrAttendancePort
import com.weeth.domain.attendance.domain.port.SseBroadcastPort
import com.weeth.domain.attendance.domain.port.SseSubscribePort
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.session.domain.repository.SessionReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Service
class SubscribeAttendanceSseUseCase(
    private val sseSubscribePort: SseSubscribePort,
    private val sseBroadcastPort: SseBroadcastPort,
    private val clubMemberPolicy: ClubMemberPolicy,
    private val sessionReader: SessionReader,
    private val qrAttendancePort: QrAttendancePort,
) {
    @Transactional(readOnly = true)
    fun execute(
        clubId: Long,
        userId: Long,
    ): SseEmitter {
        clubMemberPolicy.getActiveMember(clubId, userId)

        val emitter = sseSubscribePort.subscribe(clubId, userId)

        val openSession = sessionReader.findOpenByClubId(clubId)
        val expiredAt = openSession?.let { qrAttendancePort.getExpiredAt(it.id) }

        if (expiredAt != null) {
            sseBroadcastPort.sendToUser(clubId, userId, AttendanceSseEvent.QR_OPEN, AttendanceOpenEvent(expiredAt))
        } else {
            sseBroadcastPort.sendToUser(clubId, userId, AttendanceSseEvent.QR_NONE, null)
        }

        return emitter
    }
}
