package com.weeth.domain.notification.domain.repository

import com.weeth.domain.notification.domain.entity.NotificationOutbox
import com.weeth.domain.notification.domain.enums.NotificationDeliveryStatus
import com.weeth.domain.notification.domain.enums.NotificationOutboxStatus
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface NotificationOutboxRepository : JpaRepository<NotificationOutbox, Long> {
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
    @Query(
        value = """SELECT * FROM notification_outbox
        WHERE next_attempt_at <= :now AND (status = 'PENDING' OR (status = 'PUBLISHING' AND lease_until <= :now))
        ORDER BY notification_outbox_id LIMIT :limit FOR UPDATE SKIP LOCKED""",
        nativeQuery = true,
    )
    fun findClaimable(
        @Param("now") now: LocalDateTime,
        @Param("limit") limit: Int,
    ): List<NotificationOutbox>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
    @Query("SELECT o FROM NotificationOutbox o WHERE o.id = :id")
    fun findByIdWithLock(
        @Param("id") id: Long,
    ): NotificationOutbox?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
    @Query("SELECT o FROM NotificationOutbox o WHERE o.deliveryId = :deliveryId")
    fun findByDeliveryIdWithLock(
        @Param("deliveryId") deliveryId: Long,
    ): NotificationOutbox?

    @Query(
        """SELECT o FROM NotificationOutbox o WHERE o.status = :status AND o.publishedAt < :cutoff
        AND EXISTS (SELECT d FROM NotificationDelivery d WHERE d.id = o.deliveryId AND d.status IN :deliveryStatuses
        AND (d.leaseUntil IS NULL OR d.leaseUntil <= :now)) ORDER BY o.id""",
    )
    fun findStalePublished(
        @Param("status") status: NotificationOutboxStatus,
        @Param("deliveryStatuses") deliveryStatuses: List<NotificationDeliveryStatus>,
        @Param("cutoff") cutoff: LocalDateTime,
        @Param("now") now: LocalDateTime,
        pageable: Pageable,
    ): List<NotificationOutbox>
}
