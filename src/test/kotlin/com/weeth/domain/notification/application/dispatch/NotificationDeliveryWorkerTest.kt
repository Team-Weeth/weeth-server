package com.weeth.domain.notification.application.dispatch

import com.weeth.domain.notification.domain.port.NotificationQueuePort
import com.weeth.domain.notification.domain.vo.NotificationQueueMessage
import com.weeth.domain.notification.domain.vo.NotificationQueueReceipt
import com.weeth.global.config.NotificationDispatchProperties
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class NotificationDeliveryWorkerTest :
    StringSpec({
        "워커 수만큼만 수신하고 처리 슬롯이 비워진 후에 다음 메시지를 요청한다" {
            val queue = mockk<NotificationQueuePort>()
            val processor = mockk<NotificationDeliveryProcessor>()
            val workers = Executors.newFixedThreadPool(2)
            val poller = Executors.newSingleThreadExecutor()
            val entered = CountDownLatch(2)
            val release = CountDownLatch(1)
            val secondPoll = CountDownLatch(1)
            val calls = AtomicInteger()
            val active = AtomicInteger()
            val maximum = AtomicInteger()
            every { queue.receive(any(), any(), any(), false) } answers {
                if (calls.incrementAndGet() == 1) {
                    firstArg<Int>() shouldBe 2
                    (1L..2L).map { NotificationQueueReceipt("m$it", "r$it", NotificationQueueMessage(it, "event")) }
                } else {
                    (firstArg<Int>() in 1..2) shouldBe true
                    secondPoll.countDown()
                    CountDownLatch(1).await()
                    emptyList()
                }
            }
            every { processor.process(any()) } answers {
                maximum.accumulateAndGet(active.incrementAndGet(), ::maxOf)
                entered.countDown()
                try {
                    release.await()
                } finally {
                    active.decrementAndGet()
                }
            }
            val worker =
                NotificationDeliveryWorker(
                    queue,
                    processor,
                    NotificationDispatchProperties(workerCount = 2),
                    workers,
                    poller,
                )
            try {
                worker.start()
                entered.await(3, TimeUnit.SECONDS) shouldBe true
                calls.get() shouldBe 1
                maximum.get() shouldBe 2
                release.countDown()
                secondPoll.await(3, TimeUnit.SECONDS) shouldBe true
            } finally {
                release.countDown()
                worker.stop()
                poller.shutdownNow()
                workers.shutdownNow()
            }
            worker.isRunning shouldBe false
        }
    })
