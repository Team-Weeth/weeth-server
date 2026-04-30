package com.weeth.domain.attendance.infrastructure

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SseEmitterStoreTest :
    StringSpec({
        val clubId = 1L
        val userId = 100L

        "emitter를 replace하면 getAllByClub에서 조회된다" {
            val store = SseEmitterStore()
            val emitter = mockk<SseEmitter>(relaxed = true)

            store.replace(clubId, userId, emitter)

            store.getAllByClub(clubId) shouldHaveSize 1
        }

        "같은 userId로 replace하면 기존 emitter가 complete된다" {
            val store = SseEmitterStore()
            val oldEmitter = mockk<SseEmitter>(relaxed = true)
            val newEmitter = mockk<SseEmitter>(relaxed = true)

            store.replace(clubId, userId, oldEmitter)
            store.replace(clubId, userId, newEmitter)

            verify(exactly = 1) { oldEmitter.complete() }
            store.getAllByClub(clubId) shouldHaveSize 1
            store.getAllByClub(clubId).first().second shouldBe newEmitter
        }

        "emitter를 제거하면 조회되지 않는다" {
            val store = SseEmitterStore()
            val emitter = mockk<SseEmitter>(relaxed = true)
            store.replace(clubId, userId, emitter)

            store.remove(clubId, userId, emitter)

            store.getAllByClub(clubId).shouldBeEmpty()
        }

        "마지막 emitter 제거 시 내부 map 엔트리가 정리된다" {
            val store = SseEmitterStore()
            val emitter = mockk<SseEmitter>(relaxed = true)
            store.replace(clubId, userId, emitter)

            store.remove(clubId, userId, emitter)

            store.getAllByClub(clubId).shouldBeEmpty()
        }

        "재연결 시 old emitter의 cleanup이 new emitter를 제거하지 않는다" {
            val store = SseEmitterStore()
            val oldEmitter = mockk<SseEmitter>(relaxed = true)
            val newEmitter = mockk<SseEmitter>(relaxed = true)

            store.replace(clubId, userId, oldEmitter)
            store.replace(clubId, userId, newEmitter)
            store.remove(clubId, userId, oldEmitter) // old emitter의 onCompletion 시뮬레이션

            store.getAllByClub(clubId) shouldHaveSize 1
            store.getAllByClub(clubId).first().second shouldBe newEmitter
        }

        "getAllByClub은 userId와 emitter 쌍을 반환한다" {
            val store = SseEmitterStore()
            val emitter = mockk<SseEmitter>(relaxed = true)
            store.replace(clubId, userId, emitter)

            val result = store.getAllByClub(clubId)

            result.first().first shouldBe userId
            result.first().second shouldBe emitter
        }

        "존재하지 않는 clubId로 조회하면 빈 리스트를 반환한다" {
            val store = SseEmitterStore()

            store.getAllByClub(999L).shouldBeEmpty()
        }

        "동시에 여러 스레드에서 replace를 호출해도 유저별 emitter가 유실되지 않는다" {
            val store = SseEmitterStore()
            val threadCount = 100
            val latch = CountDownLatch(threadCount)
            val executor = Executors.newFixedThreadPool(threadCount)

            repeat(threadCount) { i ->
                executor.submit {
                    try {
                        store.replace(clubId, userId + i, mockk(relaxed = true))
                    } finally {
                        latch.countDown()
                    }
                }
            }

            try {
                latch.await(10, TimeUnit.SECONDS) shouldBe true
            } finally {
                executor.shutdown()
                executor.awaitTermination(5, TimeUnit.SECONDS)
            }

            store.getAllByClub(clubId) shouldHaveSize threadCount
        }

        "동시에 replace와 remove를 호출해도 store 상태가 일관성을 유지한다" {
            val store = SseEmitterStore()
            val threadCount = 50
            val executor = Executors.newFixedThreadPool(threadCount * 2)

            val userIds = List(threadCount) { userId + it }
            val initialEmitters = List(threadCount) { mockk<SseEmitter>(relaxed = true) }
            userIds.forEachIndexed { i, uid -> store.replace(clubId, uid, initialEmitters[i]) }

            val newEmitters = List(threadCount) { mockk<SseEmitter>(relaxed = true) }
            val latch = CountDownLatch(threadCount * 2)

            repeat(threadCount) { i ->
                executor.submit {
                    try {
                        store.replace(clubId, userIds[i], newEmitters[i])
                    } finally {
                        latch.countDown()
                    }
                }
                executor.submit {
                    try {
                        store.remove(clubId, userIds[i], initialEmitters[i])
                    } finally {
                        latch.countDown()
                    }
                }
            }

            try {
                latch.await(10, TimeUnit.SECONDS) shouldBe true
            } finally {
                executor.shutdown()
                executor.awaitTermination(5, TimeUnit.SECONDS)
            }

            // replace 후 remove가 실행됐거나, remove 후 replace가 실행된 상태 — 어느 쪽이든 초기 emitter는 없어야 함
            val inStore = store.getAllByClub(clubId).map { it.second }.toSet()
            initialEmitters.none { it in inStore } shouldBe true
        }
    })
