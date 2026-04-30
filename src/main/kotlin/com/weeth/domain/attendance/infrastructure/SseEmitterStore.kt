package com.weeth.domain.attendance.infrastructure

import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

@Component
class SseEmitterStore {
    private val store = ConcurrentHashMap<Long, ConcurrentHashMap<Long, SseEmitter>>()

    fun replace(
        clubId: Long,
        userId: Long,
        emitter: SseEmitter,
    ) {
        val oldRef = AtomicReference<SseEmitter?>()
        store.compute(clubId) { _, userMap ->
            (userMap ?: ConcurrentHashMap()).apply { oldRef.set(put(userId, emitter)) }
        }
        oldRef.get()?.complete()
    }

    fun remove(
        clubId: Long,
        userId: Long,
        emitter: SseEmitter,
    ) {
        store.computeIfPresent(clubId) { _, userMap ->
            userMap.remove(userId, emitter)
            userMap.takeUnless { it.isEmpty() }
        }
    }

    fun getByUser(
        clubId: Long,
        userId: Long,
    ): SseEmitter? = store[clubId]?.get(userId)

    fun getAllByClub(clubId: Long): List<Pair<Long, SseEmitter>> =
        store[clubId]
            ?.map { (userId, emitter) -> userId to emitter }
            ?: emptyList()
}
