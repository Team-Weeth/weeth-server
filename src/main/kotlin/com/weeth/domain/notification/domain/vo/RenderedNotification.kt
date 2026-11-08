package com.weeth.domain.notification.domain.vo

data class RenderedNotification(
    val title: String,
    val body: String,
    val templateVersion: Int,
)
