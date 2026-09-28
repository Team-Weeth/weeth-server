package com.weeth.domain.notification.domain.port

import com.weeth.domain.notification.domain.vo.PushNotificationCommand
import com.weeth.domain.notification.domain.vo.PushNotificationResult

interface PushNotificationSenderPort {
    fun sendMulticast(command: PushNotificationCommand): PushNotificationResult
}
