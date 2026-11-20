package com.weeth.domain.notification.domain.port

import com.weeth.domain.notification.domain.vo.NotificationQueueMessage
import com.weeth.domain.notification.domain.vo.NotificationQueueReceipt

/** 큐 전송 성공 ID만 반환한다. 수신 실패·삭제 실패는 호출자가 재전달을 보존하도록 예외로 알린다. */
interface NotificationQueuePort {
    fun publish(messages: List<NotificationQueueMessage>): Set<Long>

    fun receive(
        maxMessages: Int,
        waitSeconds: Int,
        visibilitySeconds: Int,
        deadLetter: Boolean = false,
    ): List<NotificationQueueReceipt>

    fun delete(receipt: NotificationQueueReceipt)

    fun changeVisibility(
        receipt: NotificationQueueReceipt,
        seconds: Int,
    )
}
