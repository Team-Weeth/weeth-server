package com.weeth.domain.attendance.domain.port

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

interface SsePort {
    fun subscribe(
        clubId: Long,
        userId: Long,
    ): SseEmitter

    fun broadcast(
        clubId: Long,
        eventName: String,
        data: Any,
    )
}
