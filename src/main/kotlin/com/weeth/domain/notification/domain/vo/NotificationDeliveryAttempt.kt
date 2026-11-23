package com.weeth.domain.notification.domain.vo

import java.time.LocalDateTime

data class NotificationDeliveryAttempt(
    val deliveryId: Long,
    val eventId: String,
    val claimToken: String,
    val userId: Long,
    val targets: List<NotificationAttemptTarget>,
    val command: PushNotificationCommand,
)

data class NotificationAttemptTarget(
    val id: Long,
    val tokenId: Long,
    val registeredAt: LocalDateTime,
    val target: PushTarget,
)
