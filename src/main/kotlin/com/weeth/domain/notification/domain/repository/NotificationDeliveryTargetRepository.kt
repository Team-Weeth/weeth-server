package com.weeth.domain.notification.domain.repository

import com.weeth.domain.notification.domain.entity.NotificationDeliveryTarget
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationDeliveryTargetRepository : JpaRepository<NotificationDeliveryTarget, Long> {
    fun findAllByDeliveryIdOrderByIdAsc(deliveryId: Long): List<NotificationDeliveryTarget>
}
