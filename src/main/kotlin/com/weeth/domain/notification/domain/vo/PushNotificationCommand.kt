package com.weeth.domain.notification.domain.vo

data class PushNotificationCommand(
    val title: String,
    val body: String,
    val targets: List<PushTarget>,
    val data: Map<String, String>,
)
