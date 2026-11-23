package com.weeth.domain.notification.application.event

import com.weeth.domain.notification.domain.vo.NotificationContent
import com.weeth.domain.notification.domain.vo.RenderedNotification

data class NotificationCreatedEvent(
    val content: NotificationContent,
    val targetUserIds: List<Long>,
    val pushMessage: RenderedNotification,
    val eventId: String,
)
