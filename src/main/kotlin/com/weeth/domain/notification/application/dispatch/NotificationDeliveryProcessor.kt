package com.weeth.domain.notification.application.dispatch

import com.weeth.domain.notification.application.usecase.command.ManageNotificationDeliveryUseCase
import com.weeth.domain.notification.domain.enums.NotificationDeliveryAction
import com.weeth.domain.notification.domain.enums.PushTargetStatus
import com.weeth.domain.notification.domain.port.NotificationQueuePort
import com.weeth.domain.notification.domain.port.PushNotificationSenderPort
import com.weeth.domain.notification.domain.vo.NotificationDeliveryAttempt
import com.weeth.domain.notification.domain.vo.NotificationDeliveryPreparation
import com.weeth.domain.notification.domain.vo.NotificationQueueReceipt
import com.weeth.domain.notification.domain.vo.PushNotificationResult
import com.weeth.domain.notification.domain.vo.PushTargetOutcome
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Component
@ConditionalOnProperty(prefix = "notification.dispatch", name = ["enabled"], havingValue = "true")
class NotificationDeliveryProcessor(
    private val management: ManageNotificationDeliveryUseCase,
    private val queue: NotificationQueuePort,
    private val sender: PushNotificationSenderPort,
    @Qualifier("notificationIoExecutor") private val io: ExecutorService,
    @Qualifier("notificationHeartbeatExecutor") private val heartbeat: ScheduledExecutorService,
    @Value("\${notification.dispatch.send-timeout-millis:20000}") private val timeoutMillis: Long = 20000,
    @Value("\${notification.dispatch.visibility-seconds:60}") private val visibilitySeconds: Int = 60,
    @Value("\${notification.dispatch.heartbeat-millis:10000}") private val heartbeatMillis: Long = 10000,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun process(receipt: NotificationQueueReceipt) {
        val message = receipt.message
        if (message == null) {
            log.warn("유효하지 않은 알림 큐 메시지. messageId={}", receipt.messageId)
            return
        }
        try {
            val preparation = management.prepare(message)
            val attempt = preparation.attempt
            val decision =
                if (preparation.action == NotificationDeliveryAction.READY && attempt != null) {
                    management.complete(attempt, send(receipt, attempt))
                } else {
                    preparation
                }
            when (decision.action) {
                NotificationDeliveryAction.ACK -> {
                    queue.delete(receipt)
                }

                NotificationDeliveryAction.RETRY -> {
                    queue.changeVisibility(
                        receipt,
                        decision.delaySeconds.coerceAtLeast(1),
                    )
                }

                NotificationDeliveryAction.AWAIT_DLQ, NotificationDeliveryAction.INVALID -> {
                    queue.changeVisibility(
                        receipt,
                        visibilitySeconds,
                    )
                }

                NotificationDeliveryAction.READY -> {
                    Unit
                }
            }
        } catch (_: Exception) {
            // DB 반영·delete 실패 시 ack하지 않는다. 재전달 때 DB 상태로 중복 발송을 막는다.
            log.warn("알림 작업 처리 실패. deliveryId={}, messageId={}", message.deliveryId, receipt.messageId)
        }
    }

    private fun send(
        receipt: NotificationQueueReceipt,
        attempt: NotificationDeliveryAttempt,
    ): PushNotificationResult {
        val future =
            try {
                io.submit(Callable { sender.sendMulticast(attempt.command) })
            } catch (
                _: Exception,
            ) {
                return retryable(attempt, "FCM_EXECUTOR_BUSY")
            }
        val lease =
            heartbeat.scheduleAtFixedRate({
                try {
                    if (management.extendLease(attempt)) {
                        queue.changeVisibility(receipt, visibilitySeconds)
                    } else {
                        future.cancel(true)
                    }
                } catch (_: Exception) {
                    future.cancel(true)
                    log.warn("알림 lease 연장 실패. deliveryId={}", attempt.deliveryId)
                }
            }, heartbeatMillis, heartbeatMillis, TimeUnit.MILLISECONDS)
        return try {
            future.get(timeoutMillis, TimeUnit.MILLISECONDS)
        } catch (_: TimeoutException) {
            future.cancel(true)
            retryable(attempt, "FCM_TIMEOUT")
        } catch (exception: Exception) {
            future.cancel(true)
            val cause = exception.cause
            if (cause is Error) throw cause
            retryable(attempt, "FCM_EXECUTION_FAILED")
        } finally {
            lease.cancel(false)
        }
    }

    private fun retryable(
        attempt: NotificationDeliveryAttempt,
        code: String,
    ) = PushNotificationResult(
        outcomes =
            attempt.targets.map {
                PushTargetOutcome(it.target.token, PushTargetStatus.RETRYABLE, code)
            },
    )
}
