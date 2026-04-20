package com.weeth.domain.attendance.infrastructure

import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Component
class SseEmitterStore {
    private val store = ConcurrentHashMap<Long, ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>>>()

    fun add(
        clubId: Long,
        userId: Long,
        emitter: SseEmitter,
    ) {
        store
            .computeIfAbsent(clubId) { ConcurrentHashMap() }
            .computeIfAbsent(userId) { CopyOnWriteArrayList() }
            .add(emitter)
    }

    fun remove(
        clubId: Long,
        userId: Long,
        emitter: SseEmitter,
    ) {
        store.computeIfPresent(clubId) { _, userMap ->
            userMap.compute(userId) { _, emitters ->
                emitters?.apply { remove(emitter) }?.ifEmpty { null }
            }
            userMap.takeUnless { it.isEmpty() }
        }
    }

    fun getAllByClub(clubId: Long): List<Pair<Long, SseEmitter>> =
        store[clubId]
            ?.flatMap { (userId, emitters) -> emitters.map { emitter -> userId to emitter } }
            ?: emptyList()
}
