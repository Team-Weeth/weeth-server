package com.weeth.domain.notification.domain.entity

import com.weeth.domain.notification.domain.enums.NotificationTargetStatus
import com.weeth.domain.notification.domain.enums.PushTargetStatus
import com.weeth.domain.notification.domain.vo.PushTargetOutcome
import com.weeth.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(
    name = "notification_delivery_target",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_delivery_target_token",
            columnNames = ["delivery_id", "token_id"],
        ),
    ],
)
class NotificationDeliveryTarget private constructor(
    deliveryId: Long,
    tokenId: Long,
    registeredAt: LocalDateTime,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_delivery_target_id")
    var id: Long = 0
        private set

    @Column(name = "delivery_id", nullable = false)
    var deliveryId: Long = deliveryId
        private set

    @Column(name = "token_id", nullable = false)
    var tokenId: Long = tokenId
        private set

    @Column(name = "registered_at", nullable = false)
    var registeredAt: LocalDateTime = registeredAt
        private set

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    var status: NotificationTargetStatus = NotificationTargetStatus.PENDING
        private set

    @Column(name = "last_error_code", length = 100)
    var lastErrorCode: String? = null
        private set

    @Column(name = "completed_at")
    var completedAt: LocalDateTime? = null
        private set

    fun record(
        outcome: PushTargetOutcome,
        now: LocalDateTime,
    ) {
        if (status != NotificationTargetStatus.PENDING) return
        status =
            when (outcome.status) {
                PushTargetStatus.SUCCESS -> {
                    NotificationTargetStatus.SUCCEEDED
                }

                PushTargetStatus.RETRYABLE -> {
                    NotificationTargetStatus.PENDING
                }

                PushTargetStatus.PERMANENT_FAILURE, PushTargetStatus.INVALID_TOKEN -> {
                    NotificationTargetStatus.PERMANENT_FAILURE
                }
            }
        lastErrorCode = outcome.errorCode?.take(100)
        if (status != NotificationTargetStatus.PENDING) completedAt = now
    }

    fun skip(now: LocalDateTime) {
        if (status != NotificationTargetStatus.PENDING) return
        status = NotificationTargetStatus.SKIPPED
        lastErrorCode = "TOKEN_CHANGED_OR_REVOKED"
        completedAt = now
    }

    companion object {
        fun create(
            deliveryId: Long,
            tokenId: Long,
            registeredAt: LocalDateTime,
        ): NotificationDeliveryTarget {
            require(deliveryId > 0 && tokenId > 0)
            return NotificationDeliveryTarget(deliveryId, tokenId, registeredAt)
        }
    }
}
