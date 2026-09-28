package com.weeth.domain.notification.application.usecase.command

import com.weeth.domain.notification.application.event.NotificationCreatedEvent
import com.weeth.domain.notification.domain.entity.UserNotification
import com.weeth.domain.notification.domain.port.UserNotificationBulkWriter
import com.weeth.domain.notification.domain.vo.NotificationContent
import com.weeth.domain.user.domain.repository.UserReader
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateNotificationUseCase(
    private val userReader: UserReader,
    private val userNotificationBulkWriter: UserNotificationBulkWriter,
    private val eventPublisher: ApplicationEventPublisher,
) {
    @Transactional
    fun execute(
        content: NotificationContent,
        targetUserIds: List<Long>,
    ) {
        if (targetUserIds.isEmpty()) return

        val notifications =
            userReader.findAllByIds(targetUserIds).map { user ->
                UserNotification.create(
                    user = user,
                    type = content.type,
                    title = content.title,
                    body = content.body,
                    targetPath = content.targetPath,
                    clubId = content.clubId,
                    referenceType = content.referenceType,
                    referenceId = content.referenceId,
                )
            }

        userNotificationBulkWriter.saveAll(notifications)
        eventPublisher.publishEvent(NotificationCreatedEvent(content, targetUserIds))
    }
}
