package com.weeth.domain.notification.domain.repository

import com.weeth.domain.notification.domain.entity.NotificationDelivery
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param

interface NotificationDeliveryRepository : JpaRepository<NotificationDelivery, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
    @Query("SELECT d FROM NotificationDelivery d WHERE d.id = :id")
    fun findByIdWithLock(
        @Param("id") id: Long,
    ): NotificationDelivery?
}
