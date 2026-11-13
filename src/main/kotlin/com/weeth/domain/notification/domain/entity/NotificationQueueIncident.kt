package com.weeth.domain.notification.domain.entity

import com.weeth.domain.notification.domain.vo.NotificationQueueReceipt
import com.weeth.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "notification_queue_incident")
class NotificationQueueIncident private constructor(
    receipt: NotificationQueueReceipt,
    reason: String,
) : BaseEntity() {
    @Id
    @Column(name = "message_id", length = 100)
    var messageId: String = receipt.messageId
        private set

    @Column(name = "delivery_id")
    var deliveryId: Long? = receipt.message?.deliveryId
        private set

    @Column(name = "event_id", length = 150)
    var eventId: String? = receipt.message?.eventId
        private set

    @Column(nullable = false, length = 100)
    var reason: String = reason
        private set

    companion object {
        fun create(
            receipt: NotificationQueueReceipt,
            reason: String,
        ): NotificationQueueIncident {
            require(receipt.messageId.isNotBlank() && receipt.messageId.length <= 100)
            return NotificationQueueIncident(receipt, reason)
        }
    }
}
