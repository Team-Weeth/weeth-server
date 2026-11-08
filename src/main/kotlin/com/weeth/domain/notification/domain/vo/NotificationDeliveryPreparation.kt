package com.weeth.domain.notification.domain.vo

import com.weeth.domain.notification.domain.enums.NotificationDeliveryAction

data class NotificationDeliveryPreparation(
    val action: NotificationDeliveryAction,
    val attempt: NotificationDeliveryAttempt? = null,
    val delaySeconds: Int = 0,
)
