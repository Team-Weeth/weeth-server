package com.weeth.domain.attendance.infrastructure

import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Component
class SseEmitterStore {
    // add-remove 복합 연산의 원자성을 보장하기 위해 @Synchronized로 직렬화
    private val store = HashMap<Long, HashMap<Long, MutableList<SseEmitter>>>()

    @Synchronized
    fun add(
        clubId: Long,
        userId: Long,
        emitter: SseEmitter,
    ) {
        store
            .getOrPut(clubId) { HashMap() }
            .getOrPut(userId) { mutableListOf() }
            .add(emitter)
    }

    @Synchronized
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

    @Synchronized
    fun getAllByClub(clubId: Long): List<Pair<Long, SseEmitter>> =
        store[clubId]
            ?.flatMap { (userId, emitters) -> emitters.map { userId to it } }
            ?: emptyList()
}
