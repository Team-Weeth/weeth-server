package com.weeth.domain.notification.application.event

import com.weeth.domain.notification.application.usecase.command.ManageNotificationTokenUseCase
import com.weeth.domain.notification.domain.port.PushNotificationSenderPort
import com.weeth.domain.notification.domain.repository.NotificationTokenReader
import com.weeth.domain.notification.domain.vo.PushNotificationCommand
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.time.LocalDateTime

@Component
class SendPushNotificationEventListener(
    private val notificationTokenReader: NotificationTokenReader,
    private val pushNotificationSenderPort: PushNotificationSenderPort,
    private val manageNotificationTokenUseCase: ManageNotificationTokenUseCase,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: NotificationCreatedEvent) {
        try {
            send(event)
        } catch (exception: Exception) {
            log.warn(
                "알림 FCM 발송 실패. type={}, referenceType={}, referenceId={}",
                event.content.type,
                event.content.referenceType,
                event.content.referenceId,
                exception,
            )
        }
    }

    private fun send(event: NotificationCreatedEvent) {
        val targets = notificationTokenReader.findActiveTargetsByUserIds(event.targetUserIds)
        if (targets.isEmpty()) return

        val sendStartedAt = LocalDateTime.now()
        val result =
            pushNotificationSenderPort.sendMulticast(
                PushNotificationCommand(
                    title = event.content.title,
                    body = event.content.body,
                    targets = targets,
                    data = event.content.data,
                ),
            )
        manageNotificationTokenUseCase.deactivateInvalidTokens(result.invalidTokens, sendStartedAt)
    }
}
