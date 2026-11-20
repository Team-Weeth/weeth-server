package com.weeth.domain.notification.application.dispatch

import com.weeth.domain.notification.application.usecase.command.ManageNotificationDeliveryUseCase
import com.weeth.domain.notification.application.usecase.command.ManageNotificationOutboxUseCase
import com.weeth.domain.notification.domain.port.NotificationQueuePort
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "notification.dispatch", name = ["enabled"], havingValue = "true")
class NotificationDispatchMaintenance(
    private val deliveries: ManageNotificationDeliveryUseCase,
    private val outboxes: ManageNotificationOutboxUseCase,
    private val queue: NotificationQueuePort,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(
        fixedDelayString = "\${notification.dispatch.reconcile-delay-ms:60000}",
        scheduler = "notificationMaintenanceScheduler",
    )
    fun reconcile() {
        try {
            val count = outboxes.reconcileStalePublished()
            if (count > 0) log.warn("장기 미완료 알림 재게시 예약. count={}", count)
        } catch (_: Exception) {
            log.warn("장기 미완료 알림 점검 실패")
        }
    }

    @Scheduled(
        fixedDelayString = "\${notification.dispatch.dlq-audit-delay-ms:60000}",
        scheduler = "notificationMaintenanceScheduler",
    )
    fun auditDeadLetters() {
        try {
            queue.receive(10, 0, 60, deadLetter = true).forEach { receipt ->
                try {
                    deliveries.recordDeadLetter(receipt)
                    // 운영자가 확인·redrive할 수 있도록 DLQ에서는 삭제하지 않는다.
                    queue.changeVisibility(receipt, 300)
                } catch (_: Exception) {
                    log.warn("알림 DLQ 감사 기록 실패. messageId={}", receipt.messageId)
                }
            }
        } catch (_: Exception) {
            log.warn("알림 DLQ 조회 실패")
        }
    }
}
