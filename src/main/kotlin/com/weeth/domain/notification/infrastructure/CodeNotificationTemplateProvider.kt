package com.weeth.domain.notification.infrastructure

import com.weeth.domain.notification.domain.enums.NotificationChannel
import com.weeth.domain.notification.domain.enums.NotificationType
import com.weeth.domain.notification.domain.port.NotificationTemplateProvider
import com.weeth.domain.notification.domain.vo.NotificationTemplate
import org.springframework.stereotype.Component

@Component
class CodeNotificationTemplateProvider : NotificationTemplateProvider {
    private val templates =
        NotificationChannel.entries.associate { channel ->
            (NotificationType.NOTICE_CREATED to channel) to
                NotificationTemplate(
                    version = 1,
                    titleTemplate = "새 공지가 등록되었습니다",
                    bodyTemplate = "{noticeTitle}",
                    requiredVariables = setOf("noticeTitle"),
                )
        }

    override fun find(
        type: NotificationType,
        channel: NotificationChannel,
        version: Int?,
    ): NotificationTemplate? = templates[type to channel]?.takeIf { version == null || it.version == version }
}
