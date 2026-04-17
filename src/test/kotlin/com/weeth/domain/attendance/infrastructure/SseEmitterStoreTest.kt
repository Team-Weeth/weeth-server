package com.weeth.domain.attendance.infrastructure

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

class SseEmitterStoreTest :
    StringSpec({
        val clubId = 1L
        val userId = 100L

        "emitter를 추가하면 getAllByClub에서 조회된다" {
            val store = SseEmitterStore()
            val emitter = mockk<SseEmitter>(relaxed = true)

            store.add(clubId, userId, emitter)

            store.getAllByClub(clubId) shouldHaveSize 1
        }

        "같은 userId로 여러 emitter를 추가하면 멀티탭이 지원된다" {
            val store = SseEmitterStore()
            val emitter1 = mockk<SseEmitter>(relaxed = true)
            val emitter2 = mockk<SseEmitter>(relaxed = true)

            store.add(clubId, userId, emitter1)
            store.add(clubId, userId, emitter2)

            store.getAllByClub(clubId) shouldHaveSize 2
        }

        "emitter를 제거하면 조회되지 않는다" {
            val store = SseEmitterStore()
            val emitter = mockk<SseEmitter>(relaxed = true)
            store.add(clubId, userId, emitter)

            store.remove(clubId, userId, emitter)

            store.getAllByClub(clubId).shouldBeEmpty()
        }

        "마지막 emitter 제거 시 userId 키도 정리된다" {
            val store = SseEmitterStore()
            val emitter = mockk<SseEmitter>(relaxed = true)
            store.add(clubId, userId, emitter)
            store.remove(clubId, userId, emitter)

            store.getAllByClub(clubId).shouldBeEmpty()
        }

        "getAllByClub은 userId와 emitter 쌍을 반환한다" {
            val store = SseEmitterStore()
            val emitter = mockk<SseEmitter>(relaxed = true)
            store.add(clubId, userId, emitter)

            val result = store.getAllByClub(clubId)

            result.first().first shouldBe userId
            result.first().second shouldBe emitter
        }

        "존재하지 않는 clubId로 조회하면 빈 리스트를 반환한다" {
            val store = SseEmitterStore()

            store.getAllByClub(999L).shouldBeEmpty()
        }
    })
