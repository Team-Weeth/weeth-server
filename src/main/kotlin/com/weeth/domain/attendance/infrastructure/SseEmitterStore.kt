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
            .getOrPut(clubId) { ConcurrentHashMap() }
            .getOrPut(userId) { CopyOnWriteArrayList() }
            .add(emitter)
    }

    fun remove(
        clubId: Long,
        userId: Long,
        emitter: SseEmitter,
    ) {
        val userMap = store[clubId] ?: return
        val emitters = userMap[userId] ?: return
        emitters.remove(emitter)
        if (emitters.isEmpty()) userMap.remove(userId)
        if (userMap.isEmpty()) store.remove(clubId)
    }

    fun getAllByClub(clubId: Long): List<Pair<Long, SseEmitter>> =
        store[clubId]
            ?.flatMap { (userId, emitters) -> emitters.map { userId to it } }
            ?: emptyList()
}
