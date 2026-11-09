package com.weeth.domain.notification.infrastructure

import com.weeth.domain.notification.domain.enums.NotificationChannel
import com.weeth.domain.notification.domain.enums.NotificationType
import com.weeth.domain.notification.domain.port.NotificationTemplateProvider
import com.weeth.domain.notification.domain.vo.NotificationTemplate
import org.springframework.stereotype.Component

@Component
class CodeNotificationTemplateProvider : NotificationTemplateProvider {
    private val templates =
        NotificationChannel.entries
            .flatMap { channel ->
                listOf(
                    (NotificationType.NOTICE_CREATED to channel) to
                        NotificationTemplate(
                            version = 1,
                            titleTemplate = "새 공지가 등록되었습니다",
                            bodyTemplate = "{noticeTitle}",
                            requiredVariables = setOf("noticeTitle"),
                        ),
                    (NotificationType.EVENT_CREATED to channel) to
                        NotificationTemplate(
                            version = 1,
                            titleTemplate = "새 일정이 등록되었습니다",
                            bodyTemplate = "{scheduleTitle} · {startAt}",
                            requiredVariables = setOf("scheduleTitle", "startAt"),
                        ),
                    (NotificationType.SESSION_CREATED to channel) to
                        NotificationTemplate(
                            version = 1,
                            titleTemplate = "새 세션이 등록되었습니다",
                            bodyTemplate = "{scheduleTitle} · {startAt} 시작 · 총 {sessionCount}회",
                            requiredVariables = setOf("scheduleTitle", "startAt", "sessionCount"),
                        ),
                )
            }.toMap()

    override fun find(
        type: NotificationType,
        channel: NotificationChannel,
        version: Int?,
    ): NotificationTemplate? = templates[type to channel]?.takeIf { version == null || it.version == version }
}
