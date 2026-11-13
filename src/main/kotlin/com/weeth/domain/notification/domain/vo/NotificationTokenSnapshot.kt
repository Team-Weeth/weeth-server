package com.weeth.domain.notification.domain.vo

import com.weeth.domain.notification.domain.enums.NotificationPlatform
import java.time.LocalDateTime

data class NotificationTokenSnapshot(
    val id: Long,
    val token: String,
    val platform: NotificationPlatform,
    val registeredAt: LocalDateTime,
)
