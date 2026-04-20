package com.weeth.domain.attendance.infrastructure

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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
                    try {
                        store.add(clubId, userId + i, mockk(relaxed = true))
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

        "동시에 add와 remove를 호출해도 store 상태가 일관성을 유지한다" {
            val store = SseEmitterStore()
            val threadCount = 50
            val executor = Executors.newFixedThreadPool(threadCount * 2)

            // 제거할 emitter를 사전에 store에 등록해 remove가 항상 유효한 대상을 갖도록 보장
            val toRemove = List(threadCount) { mockk<SseEmitter>(relaxed = true) }
            toRemove.forEach { store.add(clubId, userId, it) }

            val toAdd = List(threadCount) { mockk<SseEmitter>(relaxed = true) }
            val latch = CountDownLatch(threadCount * 2)

            repeat(threadCount) { i ->
                executor.submit {
                    try {
                        store.add(clubId, userId, toAdd[i])
                    } finally {
                        latch.countDown()
                    }
                }
                executor.submit {
                    try {
                        store.remove(clubId, userId, toRemove[i])
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

            // O(1) 조회를 위해 Set으로 변환
            val inStore = store.getAllByClub(clubId).map { it.second }.toSet()

            toAdd.all { it in inStore } shouldBe true
            toRemove.none { it in inStore } shouldBe true
        }
    })
