package com.weeth.domain.notification.application.dispatch

import com.weeth.domain.notification.domain.port.NotificationQueuePort
import com.weeth.global.config.NotificationDispatchProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.SmartLifecycle
import org.springframework.stereotype.Component
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@Component
@ConditionalOnProperty(prefix = "notification.dispatch", name = ["enabled"], havingValue = "true")
class NotificationDeliveryWorker(
    private val queue: NotificationQueuePort,
    private val processor: NotificationDeliveryProcessor,
    private val properties: NotificationDispatchProperties,
    @Qualifier("notificationWorkerExecutor") private val workers: ExecutorService,
    @Qualifier("notificationPollExecutor") private val poller: ExecutorService,
) : SmartLifecycle {
    private val log = LoggerFactory.getLogger(javaClass)
    private val running = AtomicBoolean(false)
    private val permits = Semaphore(properties.workerCount)
    private var polling: Future<*>? = null

    override fun start() {
        if (!running.compareAndSet(false, true)) return
        polling =
            poller.submit {
                while (running.get() && !Thread.currentThread().isInterrupted) {
                    try {
                        val capacity = permits.availablePermits().coerceAtMost(properties.receiveBatchSize)
                        if (capacity == 0) {
                            TimeUnit.MILLISECONDS.sleep(50)
                            continue
                        }
                        val receipts =
                            queue.receive(
                                capacity,
                                properties.longPollingSeconds,
                                properties.visibilitySeconds,
                            )
                        receipts.forEach { receipt ->
                            if (!running.get()) return@forEach
                            permits.acquire()
                            try {
                                workers.execute {
                                    try {
                                        processor.process(receipt)
                                    } finally {
                                        permits.release()
                                    }
                                }
                            } catch (exception: Exception) {
                                permits.release()
                                throw exception
                            }
                        }
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    } catch (_: Exception) {
                        log.warn("알림 큐 수신 실패")
                        try {
                            TimeUnit.SECONDS.sleep(1)
                        } catch (
                            _: InterruptedException,
                        ) {
                            Thread.currentThread().interrupt()
                        }
                    }
                }
            }
    }

    override fun stop() {
        running.set(false)
        polling?.cancel(true)
        workers.shutdown()
        try {
            workers.awaitTermination(30, TimeUnit.SECONDS)
        } catch (
            _: InterruptedException,
        ) {
            Thread.currentThread().interrupt()
        }
    }

    override fun isRunning(): Boolean = running.get()

    override fun getPhase(): Int = Int.MAX_VALUE - 100
}
