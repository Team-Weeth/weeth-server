package com.weeth.domain.notification.domain.vo

sealed interface NotificationDeliveryMessage {
    data class Ready(
        val message: RenderedNotification,
    ) : NotificationDeliveryMessage

    data class Skip(
        val reason: String,
    ) : NotificationDeliveryMessage
}
