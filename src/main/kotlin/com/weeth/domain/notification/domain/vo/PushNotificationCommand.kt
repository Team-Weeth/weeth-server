package com.weeth.domain.notification.domain.vo

data class PushNotificationCommand(
    val title: String,
    val body: String,
    val tokens: List<String>,
    val data: Map<String, String>,
)
