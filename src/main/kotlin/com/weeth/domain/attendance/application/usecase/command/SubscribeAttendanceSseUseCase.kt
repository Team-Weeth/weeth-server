package com.weeth.domain.attendance.application.usecase.command

import com.weeth.domain.attendance.domain.port.SseSubscribePort
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Service
class SubscribeAttendanceSseUseCase(
    private val sseSubscribePort: SseSubscribePort,
    private val clubMemberPolicy: ClubMemberPolicy,
) {
    @Transactional
    fun execute(
        clubId: Long,
        userId: Long,
    ): SseEmitter {
        clubMemberPolicy.getActiveMember(clubId, userId)
        return sseSubscribePort.subscribe(clubId, userId)
    }
}
