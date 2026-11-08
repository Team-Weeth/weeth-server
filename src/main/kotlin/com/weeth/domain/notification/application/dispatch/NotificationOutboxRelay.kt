package com.weeth.domain.notification.application.dispatch

import com.weeth.domain.notification.application.usecase.command.ManageNotificationOutboxUseCase
import com.weeth.domain.notification.domain.port.NotificationQueuePort
import com.weeth.domain.notification.domain.vo.NotificationQueueMessage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "notification.dispatch", name = ["enabled"], havingValue = "true")
class NotificationOutboxRelay(
    private val management: ManageNotificationOutboxUseCase,
    private val queue: NotificationQueuePort,
    @Value("\${notification.dispatch.relay-batch-size:100}") private val batchSize: Int = 100,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(
        fixedDelayString = "\${notification.dispatch.relay-delay-ms:1000}",
        scheduler = "notificationMaintenanceScheduler",
    )
    fun runOnce() {
        try {
            val batch = management.claimBatch(batchSize)
            if (batch.isEmpty()) return
            val successes =
                try {
                    queue.publish(batch.map { NotificationQueueMessage(it.deliveryId, it.eventId) })
                } catch (_: Exception) {
                    log.warn("알림 Outbox 게시 실패. batchSize={}", batch.size)
                    emptySet()
                }
            batch.forEach { dispatch ->
                try {
                    management.completePublish(dispatch, dispatch.deliveryId in successes)
                } catch (
                    _: Exception,
                ) {
                    log.warn("알림 Outbox 게시 결과 반영 실패. outboxId={}", dispatch.outboxId)
                }
            }
        } catch (_: Exception) {
            log.warn("알림 Outbox 선점 실패")
        }
    }
}
