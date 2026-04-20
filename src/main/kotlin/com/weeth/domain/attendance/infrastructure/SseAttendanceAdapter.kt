package com.weeth.domain.attendance.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import com.weeth.domain.attendance.domain.port.SseBroadcastPort
import com.weeth.domain.attendance.domain.port.SseSubscribePort
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Component
class SseAttendanceAdapter(
    private val store: SseEmitterStore,
    private val objectMapper: ObjectMapper,
) : SseBroadcastPort,
    SseSubscribePort {
    companion object {
        private const val TIMEOUT = 30 * 60 * 1000L
        private const val EVENT_CONNECT = "connect"
    }

    override fun subscribe(
        clubId: Long,
        userId: Long,
    ): SseEmitter {
        val emitter = SseEmitter(TIMEOUT)
        val cleanup = { store.remove(clubId, userId, emitter) }

        store.add(clubId, userId, emitter)
        emitter.onCompletion(cleanup)
        emitter.onTimeout(cleanup)
        emitter.onError { cleanup() }

        runCatching {
            emitter.send(SseEmitter.event().name(EVENT_CONNECT).data("connected"))
        }.onFailure { cleanup() }

        return emitter
    }

    override fun broadcast(
        clubId: Long,
        eventName: String,
        data: Any,
    ) {
        val payload = runCatching { objectMapper.writeValueAsString(data) }.getOrElse { return }

        store.getAllByClub(clubId).forEach { (userId, emitter) ->
            val cleanup = { store.remove(clubId, userId, emitter) }
            runCatching {
                emitter.send(SseEmitter.event().name(eventName).data(payload))
            }.onFailure { cleanup() }
        }
    }
}
