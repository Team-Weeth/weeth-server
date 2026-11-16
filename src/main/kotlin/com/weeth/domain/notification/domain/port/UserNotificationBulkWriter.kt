package com.weeth.domain.notification.domain.port

import com.weeth.domain.notification.domain.entity.UserNotification

fun interface UserNotificationBulkWriter {
    fun saveAll(notifications: List<UserNotification>)
}
