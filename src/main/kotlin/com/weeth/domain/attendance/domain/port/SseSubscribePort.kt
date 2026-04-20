package com.weeth.domain.attendance.domain.port

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

interface SseSubscribePort {
    fun subscribe(
        clubId: Long,
        userId: Long,
    ): SseEmitter
}
