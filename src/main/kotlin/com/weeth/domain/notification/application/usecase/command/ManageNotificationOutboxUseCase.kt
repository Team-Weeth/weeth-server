package com.weeth.domain.notification.application.usecase.command

import com.weeth.domain.notification.domain.enums.NotificationDeliveryStatus
import com.weeth.domain.notification.domain.enums.NotificationOutboxStatus
import com.weeth.domain.notification.domain.repository.NotificationOutboxRepository
import com.weeth.domain.notification.domain.service.NotificationRetryPolicy
import com.weeth.domain.notification.domain.vo.NotificationOutboxDispatch
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Service
class ManageNotificationOutboxUseCase(
    private val repository: NotificationOutboxRepository,
    private val retryPolicy: NotificationRetryPolicy,
    private val clock: Clock,
    @Value("\${notification.dispatch.lease-seconds:60}") private val leaseSeconds: Long = 60,
) {
    @Transactional
    fun claimBatch(limit: Int = 10): List<NotificationOutboxDispatch> {
        val now = LocalDateTime.now(clock)
        return repository.findClaimable(now, limit.coerceIn(1, 10)).map { outbox ->
            val token = UUID.randomUUID().toString()
            outbox.claim(token, now, leaseSeconds)
            NotificationOutboxDispatch(outbox.id, outbox.deliveryId, outbox.eventId, token, outbox.generation)
        }
    }

    @Transactional
    fun completePublish(
        dispatch: NotificationOutboxDispatch,
        success: Boolean,
    ) {
        val outbox = repository.findByIdWithLock(dispatch.outboxId) ?: return
        outbox.complete(
            dispatch.claimToken,
            success,
            LocalDateTime.now(clock),
            retryPolicy.outboxDelaySeconds(outbox.attemptCount),
        )
    }

    @Transactional
    fun reconcileStalePublished(): Int {
        val now = LocalDateTime.now(clock)
        val stale =
            repository.findStalePublished(
                NotificationOutboxStatus.PUBLISHED,
                listOf(
                    NotificationDeliveryStatus.PENDING,
                    NotificationDeliveryStatus.RETRY_WAIT,
                    NotificationDeliveryStatus.PROCESSING,
                ),
                now.minusHours(1),
                now,
                PageRequest.of(0, 100),
            )
        var requeued = 0
        stale.forEach { candidate ->
            val outbox = repository.findByIdWithLock(candidate.id)
            if (outbox?.status == NotificationOutboxStatus.PUBLISHED &&
                outbox.publishedAt?.isBefore(now.minusHours(1)) == true
            ) {
                outbox.requeue(now)
                requeued++
            }
        }
        return requeued
    }
}
