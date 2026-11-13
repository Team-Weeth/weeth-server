package com.weeth.domain.notification.domain.vo

data class NotificationQueueMessage(
    val deliveryId: Long,
    val eventId: String,
    val schemaVersion: Int = 2,
    val generation: Long = 0,
)
