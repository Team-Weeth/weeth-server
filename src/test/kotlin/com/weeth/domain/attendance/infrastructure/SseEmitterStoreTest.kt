package com.weeth.domain.attendance.infrastructure

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

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

        "마지막 emitter 제거 시 내부 map 엔트리가 정리된다" {
            val store = SseEmitterStore()
            val emitter = mockk<SseEmitter>(relaxed = true)
            store.add(clubId, userId, emitter)

            store.remove(clubId, userId, emitter)

            store.getAllByClub(clubId).shouldBeEmpty()
        }

        "여러 emitter 중 하나만 제거하면 나머지는 유지된다" {
            val store = SseEmitterStore()
            val emitter1 = mockk<SseEmitter>(relaxed = true)
            val emitter2 = mockk<SseEmitter>(relaxed = true)
            store.add(clubId, userId, emitter1)
            store.add(clubId, userId, emitter2)

            store.remove(clubId, userId, emitter1)

            store.getAllByClub(clubId) shouldHaveSize 1
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

        "동시에 여러 스레드에서 add를 호출해도 emitter가 유실되지 않는다" {
            val store = SseEmitterStore()
            val threadCount = 100
            val latch = CountDownLatch(threadCount)
            val executor = Executors.newFixedThreadPool(threadCount)

            repeat(threadCount) { i ->
                executor.submit {
                    store.add(clubId, userId + i, mockk(relaxed = true))
                    latch.countDown()
                }
            }

            latch.await()
            executor.shutdown()

            store.getAllByClub(clubId) shouldHaveSize threadCount
        }

        "동시에 add와 remove를 호출해도 store 상태가 일관성을 유지한다" {
            val store = SseEmitterStore()
            val threadCount = 50
            val latch = CountDownLatch(threadCount * 2)
            val executor = Executors.newFixedThreadPool(threadCount * 2)
            val addedEmitters = CopyOnWriteArrayList<SseEmitter>()
            val removedEmitters = CopyOnWriteArrayList<SseEmitter>()
            val removeIndex = AtomicInteger(0)

            repeat(threadCount) {
                executor.submit {
                    val emitter = mockk<SseEmitter>(relaxed = true)
                    store.add(clubId, userId, emitter)
                    addedEmitters.add(emitter)
                    latch.countDown()
                }
                executor.submit {
                    val idx = removeIndex.getAndIncrement()
                    val target = addedEmitters.getOrNull(idx)
                    if (target != null) {
                        store.remove(clubId, userId, target)
                        removedEmitters.add(target)
                    }
                    latch.countDown()
                }
            }

            latch.await()
            executor.shutdown()

            val inStore = store.getAllByClub(clubId).map { it.second }.toSet()

            inStore.all { it in addedEmitters } shouldBe true
            removedEmitters.none { it in inStore } shouldBe true
        }
    })
