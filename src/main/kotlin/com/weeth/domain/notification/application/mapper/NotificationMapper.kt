package com.weeth.domain.notification.application.mapper

import com.weeth.domain.notification.application.dto.response.NotificationResponse
import com.weeth.domain.notification.domain.entity.UserNotification
import com.weeth.global.common.id.TsidBase62Encoder
import org.springframework.stereotype.Component

@Component
class NotificationMapper {
    fun toResponse(notification: UserNotification): NotificationResponse =
        NotificationResponse(
            id = notification.id,
            type = notification.type,
            title = notification.title,
            body = notification.body,
            targetPath = notification.targetPath,
            clubId = TsidBase62Encoder.encode(notification.clubId),
            referenceType = notification.referenceType,
            referenceId = notification.referenceId,
            isRead = notification.isRead,
            createdAt = notification.createdAt,
            readAt = notification.readAt,
        )
}
