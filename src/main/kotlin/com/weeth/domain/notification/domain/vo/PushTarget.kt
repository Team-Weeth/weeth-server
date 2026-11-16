package com.weeth.domain.notification.domain.vo

import com.weeth.domain.notification.domain.enums.NotificationPlatform

data class PushTarget(
    val token: String,
    val platform: NotificationPlatform,
)
