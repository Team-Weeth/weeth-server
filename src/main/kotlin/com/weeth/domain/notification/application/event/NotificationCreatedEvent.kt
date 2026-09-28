package com.weeth.domain.notification.application.event

import com.weeth.domain.notification.domain.vo.NotificationContent

data class NotificationCreatedEvent(
    val content: NotificationContent,
    val targetUserIds: List<Long>,
)
