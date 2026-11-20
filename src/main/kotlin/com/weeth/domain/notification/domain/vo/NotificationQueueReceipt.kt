package com.weeth.domain.notification.domain.vo

data class NotificationQueueReceipt(
    val messageId: String,
    val receiptHandle: String,
    val message: NotificationQueueMessage?,
    val receiveCount: Int = 1,
    val deadLetter: Boolean = false,
)
