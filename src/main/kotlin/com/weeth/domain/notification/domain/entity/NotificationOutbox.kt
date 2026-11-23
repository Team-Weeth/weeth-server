package com.weeth.domain.notification.domain.entity

import com.weeth.domain.notification.domain.enums.NotificationOutboxStatus
import com.weeth.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(
    name = "notification_outbox",
    uniqueConstraints = [UniqueConstraint(name = "uk_outbox_delivery", columnNames = ["delivery_id"])],
    indexes = [Index(name = "idx_outbox_due", columnList = "status, next_attempt_at")],
)
class NotificationOutbox private constructor(
    deliveryId: Long,
    eventId: String,
    now: LocalDateTime,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_outbox_id")
    var id: Long = 0
        private set

    @Column(name = "delivery_id", nullable = false)
    var deliveryId: Long = deliveryId
        private set

    @Column(name = "event_id", nullable = false, length = 150)
    var eventId: String = eventId
        private set

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    var status: NotificationOutboxStatus = NotificationOutboxStatus.PENDING
        private set

    @Column(name = "attempt_count", nullable = false)
    var attemptCount: Int = 0
        private set

    @Column(name = "next_attempt_at", nullable = false)
    var nextAttemptAt: LocalDateTime = now
        private set

    @Column(name = "claim_token", length = 36)
    var claimToken: String? = null
        private set

    @Column(name = "lease_until")
    var leaseUntil: LocalDateTime? = null
        private set

    @Column(name = "published_at")
    var publishedAt: LocalDateTime? = null
        private set

    @Column(name = "last_error_code", length = 100)
    var lastErrorCode: String? = null
        private set

    @Column(nullable = false)
    var generation: Long = 0
        private set

    fun claim(
        token: String,
        now: LocalDateTime,
        leaseSeconds: Long,
    ) {
        check(status != NotificationOutboxStatus.PUBLISHED && !nextAttemptAt.isAfter(now))
        check(status != NotificationOutboxStatus.PUBLISHING || leaseUntil?.isAfter(now) != true)
        status = NotificationOutboxStatus.PUBLISHING
        claimToken = token
        leaseUntil = now.plusSeconds(leaseSeconds)
        attemptCount++
    }

    fun complete(
        token: String,
        success: Boolean,
        now: LocalDateTime,
        retryDelaySeconds: Long,
    ): Boolean {
        if (status != NotificationOutboxStatus.PUBLISHING || claimToken != token ||
            leaseUntil?.isAfter(now) != true
        ) {
            return false
        }
        status = if (success) NotificationOutboxStatus.PUBLISHED else NotificationOutboxStatus.PENDING
        if (success) publishedAt = now else nextAttemptAt = now.plusSeconds(retryDelaySeconds)
        lastErrorCode = if (success) null else "SQS_PUBLISH_FAILED"
        claimToken = null
        leaseUntil = null
        return true
    }

    fun requeue(
        now: LocalDateTime,
        generation: Long = this.generation,
    ) {
        require(generation >= this.generation)
        this.generation = generation
        status = NotificationOutboxStatus.PENDING
        nextAttemptAt = now
        claimToken = null
        leaseUntil = null
        publishedAt = null
    }

    companion object {
        fun create(
            deliveryId: Long,
            eventId: String,
            now: LocalDateTime,
        ): NotificationOutbox {
            require(deliveryId > 0 && eventId.isNotBlank() && eventId.length <= 150)
            return NotificationOutbox(deliveryId, eventId, now)
        }
    }
}
