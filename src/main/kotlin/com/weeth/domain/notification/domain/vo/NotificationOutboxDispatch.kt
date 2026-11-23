package com.weeth.domain.notification.domain.vo

data class NotificationOutboxDispatch(
    val outboxId: Long,
    val deliveryId: Long,
    val eventId: String,
    val claimToken: String,
    val generation: Long = 0,
)
