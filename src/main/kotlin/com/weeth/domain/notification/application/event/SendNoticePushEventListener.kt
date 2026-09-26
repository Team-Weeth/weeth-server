package com.weeth.domain.notification.application.event

import com.weeth.domain.board.application.event.NoticeCreatedEvent
import com.weeth.domain.notification.application.usecase.command.ManageNotificationTokenUseCase
import com.weeth.domain.notification.domain.enums.NotificationType
import com.weeth.domain.notification.domain.port.PushNotificationSenderPort
import com.weeth.domain.notification.domain.repository.NotificationTokenReader
import com.weeth.domain.notification.domain.repository.UserNotificationReader
import com.weeth.domain.notification.domain.vo.PushNotificationCommand
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.time.LocalDateTime

@Component
class SendNoticePushEventListener(
    private val userNotificationReader: UserNotificationReader,
    private val notificationTokenReader: NotificationTokenReader,
    private val pushNotificationSenderPort: PushNotificationSenderPort,
    private val manageNotificationTokenUseCase: ManageNotificationTokenUseCase,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: NoticeCreatedEvent) {
        try {
            send(event)
        } catch (e: Exception) {
            log.warn("공지 알림 FCM 발송 실패. postId={}", event.postId, e)
        }
    }

    private fun send(event: NoticeCreatedEvent) {
        val targetUserIds =
            userNotificationReader.findTargetUserIdsByTypeAndPostId(
                type = NotificationType.NOTICE_CREATED,
                postId = event.postId,
            )
        if (targetUserIds.isEmpty()) {
            return
        }

        val tokens = notificationTokenReader.findActiveTokensByUserIds(targetUserIds)
        if (tokens.isEmpty()) {
            return
        }

        val sendStartedAt = LocalDateTime.now()
        val result = pushNotificationSenderPort.sendMulticast(createCommand(event, tokens))
        manageNotificationTokenUseCase.deactivateInvalidTokens(
            invalidTokens = result.invalidTokens,
            registeredBeforeOrAt = sendStartedAt,
        )
    }

    private fun createCommand(
        event: NoticeCreatedEvent,
        tokens: List<String>,
    ): PushNotificationCommand {
        val targetPath = createTargetPath(event)
        return PushNotificationCommand(
            title = NOTICE_CREATED_NOTIFICATION_TITLE,
            body = event.title,
            tokens = tokens,
            data =
                mapOf(
                    "type" to NotificationType.NOTICE_CREATED.name,
                    "clubId" to event.clubId.toString(),
                    "boardId" to event.boardId.toString(),
                    "postId" to event.postId.toString(),
                    "targetPath" to targetPath,
                ),
        )
    }

    private fun createTargetPath(event: NoticeCreatedEvent): String =
        "/clubs/${event.clubId}/boards/${event.boardId}/posts/${event.postId}"

    private companion object {
        const val NOTICE_CREATED_NOTIFICATION_TITLE = "새 공지가 등록되었습니다"
    }
}
