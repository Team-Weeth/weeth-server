package com.weeth.domain.notification.domain.entity

import com.weeth.domain.notification.domain.enums.NotificationChannel
import com.weeth.domain.notification.domain.enums.NotificationDeliveryAction
import com.weeth.domain.notification.domain.enums.NotificationDeliveryStatus
import com.weeth.domain.notification.domain.enums.NotificationTargetStatus
import com.weeth.domain.notification.domain.enums.NotificationType
import com.weeth.domain.notification.domain.service.NotificationRetryPolicy
import com.weeth.domain.notification.domain.vo.NotificationDeliveryPreparation
import com.weeth.domain.notification.domain.vo.RenderedNotification
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
import java.time.Duration
import java.time.LocalDateTime

@Entity
@Table(
    name = "notification_delivery",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_delivery_event_user_channel",
            columnNames = ["event_id", "user_id", "channel"],
        ),
    ],
    indexes = [
        Index(
            name = "idx_delivery_due",
            columnList = "status, next_attempt_at",
        ), Index(name = "idx_delivery_lease", columnList = "status, lease_until"),
    ],
)
class NotificationDelivery private constructor(
    eventId: String,
    userId: Long,
    clubId: Long,
    type: NotificationType,
    message: RenderedNotification,
    data: Map<String, String>,
    now: LocalDateTime,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_delivery_id")
    var id: Long = 0
        private set

    @Column(name = "event_id", nullable = false, length = 150)
    var eventId: String = eventId
        private set

    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        private set

    @Column(name = "club_id", nullable = false)
    var clubId: Long = clubId
        private set

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    var type: NotificationType = type
        private set

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    var channel: NotificationChannel = NotificationChannel.PUSH
        private set

    @Column(nullable = false, length = 100)
    var title: String = message.title
        private set

    @Column(nullable = false, length = 255)
    var body: String = message.body
        private set

    @Column(name = "template_version", nullable = false)
    var templateVersion: Int = message.templateVersion
        private set

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_data", nullable = false, columnDefinition = "json")
    var data: Map<String, String> = data.toMap()
        private set

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    var status: NotificationDeliveryStatus = NotificationDeliveryStatus.PENDING
        private set

    @Column(name = "attempt_count", nullable = false)
    var attemptCount: Int = 0
        private set

    @Column(name = "targets_initialized", nullable = false)
    var targetsInitialized: Boolean = false
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

    @Column(name = "completed_at")
    var completedAt: LocalDateTime? = null
        private set

    @Column(name = "dead_lettered_at")
    var deadLetteredAt: LocalDateTime? = null
        private set

    @Column(name = "last_error_code", length = 100)
    var lastErrorCode: String? = null
        private set

    val awaitingDeadLetter: Boolean
        get() =
            status == NotificationDeliveryStatus.FAILED && lastErrorCode == "RETRY_EXHAUSTED" &&
                deadLetteredAt == null

    fun decision(now: LocalDateTime): NotificationDeliveryPreparation =
        when {
            status in
                setOf(
                    NotificationDeliveryStatus.SUCCEEDED,
                    NotificationDeliveryStatus.SKIPPED,
                )
            -> {
                NotificationDeliveryPreparation(NotificationDeliveryAction.ACK)
            }

            status == NotificationDeliveryStatus.FAILED -> {
                NotificationDeliveryPreparation(
                    if (awaitingDeadLetter) NotificationDeliveryAction.AWAIT_DLQ else NotificationDeliveryAction.ACK,
                )
            }

            status == NotificationDeliveryStatus.PROCESSING && leaseUntil?.isAfter(
                now,
            ) == true -> {
                waitUntil(checkNotNull(leaseUntil), now)
            }

            status == NotificationDeliveryStatus.RETRY_WAIT && nextAttemptAt.isAfter(now) -> {
                waitUntil(nextAttemptAt, now)
            }

            else -> {
                NotificationDeliveryPreparation(NotificationDeliveryAction.READY)
            }
        }

    fun claim(
        token: String,
        now: LocalDateTime,
        leaseSeconds: Long,
    ) {
        check(decision(now).action == NotificationDeliveryAction.READY)
        require(token.isNotBlank() && leaseSeconds > 0)
        status = NotificationDeliveryStatus.PROCESSING
        claimToken = token
        leaseUntil = now.plusSeconds(leaseSeconds)
    }

    fun hasClaim(
        token: String,
        now: LocalDateTime,
    ): Boolean =
        status == NotificationDeliveryStatus.PROCESSING && claimToken == token && leaseUntil?.isAfter(now) == true

    fun extend(
        token: String,
        now: LocalDateTime,
        leaseSeconds: Long,
    ): Boolean {
        if (!hasClaim(token, now)) return false
        leaseUntil = now.plusSeconds(leaseSeconds)
        return true
    }

    fun initializeTargets() {
        targetsInitialized = true
    }

    fun beginAttempt() {
        check(status == NotificationDeliveryStatus.PROCESSING)
        attemptCount++
    }

    fun skip(
        now: LocalDateTime,
        reason: String,
    ) {
        status = NotificationDeliveryStatus.SKIPPED
        lastErrorCode = reason
        complete(now)
    }

    fun exhaust(now: LocalDateTime) {
        status = NotificationDeliveryStatus.FAILED
        lastErrorCode = "RETRY_EXHAUSTED"
        complete(now)
    }

    fun finishAttempt(
        token: String,
        now: LocalDateTime,
        targets: List<NotificationDeliveryTarget>,
        policy: NotificationRetryPolicy,
    ): Boolean {
        if (!hasClaim(token, now)) return false
        when {
            targets.any { it.status == NotificationTargetStatus.PENDING } -> {
                if (attemptCount >= policy.maxAttempts) {
                    exhaust(now)
                } else {
                    status = NotificationDeliveryStatus.RETRY_WAIT
                    nextAttemptAt = now.plusSeconds(policy.delaySeconds(attemptCount))
                    lastErrorCode = "RETRYABLE_TARGET_FAILURE"
                    clearClaim()
                }
            }

            targets.any { it.status == NotificationTargetStatus.PERMANENT_FAILURE } -> {
                status = NotificationDeliveryStatus.FAILED
                lastErrorCode = "PERMANENT_TARGET_FAILURE"
                complete(now)
            }

            targets.any { it.status == NotificationTargetStatus.SUCCEEDED } -> {
                status = NotificationDeliveryStatus.SUCCEEDED
                lastErrorCode = null
                complete(now)
            }

            else -> {
                skip(now, "NO_ACTIVE_TARGET")
            }
        }
        return true
    }

    fun markDeadLetter(now: LocalDateTime) {
        if (status in setOf(NotificationDeliveryStatus.SUCCEEDED, NotificationDeliveryStatus.SKIPPED)) return
        status = NotificationDeliveryStatus.FAILED
        deadLetteredAt = now
        lastErrorCode = lastErrorCode ?: "DLQ_RECEIVE_LIMIT"
        complete(now)
    }

    fun requeue(now: LocalDateTime) {
        check(status == NotificationDeliveryStatus.FAILED && (awaitingDeadLetter || deadLetteredAt != null))
        status = NotificationDeliveryStatus.PENDING
        attemptCount = 0
        nextAttemptAt = now
        completedAt = null
        deadLetteredAt = null
        lastErrorCode = null
        clearClaim()
    }

    private fun complete(now: LocalDateTime) {
        completedAt = now
        clearClaim()
    }

    private fun clearClaim() {
        claimToken = null
        leaseUntil = null
    }

    private fun waitUntil(
        time: LocalDateTime,
        now: LocalDateTime,
    ) = NotificationDeliveryPreparation(
        NotificationDeliveryAction.RETRY,
        delaySeconds =
            Duration
                .between(now, time)
                .seconds
                .coerceIn(1, 43200)
                .toInt(),
    )

    companion object {
        fun create(
            eventId: String,
            userId: Long,
            clubId: Long,
            type: NotificationType,
            message: RenderedNotification,
            data: Map<String, String>,
            now: LocalDateTime,
        ): NotificationDelivery {
            require(eventId.isNotBlank() && eventId.length <= 150 && userId > 0 && clubId > 0)
            require(
                message.title.isNotBlank() && message.title.length <= 100 && message.body.isNotBlank() &&
                    message.body.length <= 255 &&
                    message.templateVersion > 0,
            )
            return NotificationDelivery(eventId, userId, clubId, type, message, data, now)
        }
    }
}
