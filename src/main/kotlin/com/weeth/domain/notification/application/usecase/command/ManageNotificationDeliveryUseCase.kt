package com.weeth.domain.notification.application.usecase.command

import com.weeth.domain.notification.domain.entity.NotificationDeliveryTarget
import com.weeth.domain.notification.domain.entity.NotificationQueueIncident
import com.weeth.domain.notification.domain.enums.NotificationDeliveryAction
import com.weeth.domain.notification.domain.enums.NotificationTargetStatus
import com.weeth.domain.notification.domain.enums.PushTargetStatus
import com.weeth.domain.notification.domain.repository.NotificationDeliveryRepository
import com.weeth.domain.notification.domain.repository.NotificationDeliveryTargetRepository
import com.weeth.domain.notification.domain.repository.NotificationOutboxRepository
import com.weeth.domain.notification.domain.repository.NotificationQueueIncidentRepository
import com.weeth.domain.notification.domain.repository.NotificationTokenRepository
import com.weeth.domain.notification.domain.service.NotificationRecipientPolicy
import com.weeth.domain.notification.domain.service.NotificationRetryPolicy
import com.weeth.domain.notification.domain.vo.NotificationAttemptTarget
import com.weeth.domain.notification.domain.vo.NotificationDeliveryAttempt
import com.weeth.domain.notification.domain.vo.NotificationDeliveryPreparation
import com.weeth.domain.notification.domain.vo.NotificationQueueMessage
import com.weeth.domain.notification.domain.vo.NotificationQueueReceipt
import com.weeth.domain.notification.domain.vo.PushNotificationCommand
import com.weeth.domain.notification.domain.vo.PushNotificationResult
import com.weeth.domain.notification.domain.vo.PushTarget
import com.weeth.domain.notification.domain.vo.PushTargetOutcome
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Service
class ManageNotificationDeliveryUseCase(
    private val deliveryRepository: NotificationDeliveryRepository,
    private val targetRepository: NotificationDeliveryTargetRepository,
    private val tokenRepository: NotificationTokenRepository,
    private val outboxRepository: NotificationOutboxRepository,
    private val incidentRepository: NotificationQueueIncidentRepository,
    private val recipientPolicy: NotificationRecipientPolicy,
    private val retryPolicy: NotificationRetryPolicy,
    private val clock: Clock,
    @Value("\${notification.dispatch.lease-seconds:60}") private val leaseSeconds: Long = 60,
) {
    @Transactional
    fun prepare(message: NotificationQueueMessage): NotificationDeliveryPreparation {
        val now = LocalDateTime.now(clock)
        val delivery =
            deliveryRepository.findByIdWithLock(message.deliveryId)
                ?: return NotificationDeliveryPreparation(NotificationDeliveryAction.INVALID)
        if (delivery.eventId != message.eventId || message.schemaVersion != 1) {
            return NotificationDeliveryPreparation(NotificationDeliveryAction.INVALID)
        }
        val decision = delivery.decision(now)
        if (decision.action != NotificationDeliveryAction.READY) return decision
        val claim = UUID.randomUUID().toString()
        delivery.claim(claim, now, leaseSeconds)
        if (recipientPolicy
                .filterEligibleUserIds(
                    delivery.clubId,
                    delivery.type.category,
                    listOf(delivery.userId),
                ).isEmpty()
        ) {
            delivery.skip(now, "RECIPIENT_NO_LONGER_ELIGIBLE")
            return delivery.decision(now)
        }
        val currentTokens = tokenRepository.findActiveTokenSnapshotsByUserId(delivery.userId).associateBy { it.id }
        val targets =
            if (!delivery.targetsInitialized) {
                delivery.initializeTargets()
                targetRepository.saveAll(
                    currentTokens.values.map {
                        NotificationDeliveryTarget.create(delivery.id, it.id, it.registeredAt)
                    },
                )
            } else {
                targetRepository.findAllByDeliveryIdOrderByIdAsc(delivery.id)
            }
        val ready =
            targets.filter { it.status == NotificationTargetStatus.PENDING }.mapNotNull { target ->
                val current = currentTokens[target.tokenId]
                if (current == null || current.registeredAt != target.registeredAt) {
                    target.skip(now)
                    null
                } else {
                    NotificationAttemptTarget(
                        target.id,
                        target.tokenId,
                        target.registeredAt,
                        PushTarget(current.token, current.platform),
                    )
                }
            }
        if (ready.isEmpty()) {
            delivery.finishAttempt(claim, now, targets, retryPolicy)
            return delivery.decision(now)
        }
        if (delivery.attemptCount >= retryPolicy.maxAttempts) {
            delivery.exhaust(now)
            return delivery.decision(now)
        }
        delivery.beginAttempt()
        return NotificationDeliveryPreparation(
            NotificationDeliveryAction.READY,
            NotificationDeliveryAttempt(
                delivery.id,
                delivery.eventId,
                claim,
                delivery.userId,
                ready,
                PushNotificationCommand(delivery.title, delivery.body, ready.map { it.target }, delivery.data.toMap()),
            ),
        )
    }

    @Transactional
    fun complete(
        attempt: NotificationDeliveryAttempt,
        result: PushNotificationResult,
    ): NotificationDeliveryPreparation {
        val now = LocalDateTime.now(clock)
        val delivery =
            deliveryRepository.findByIdWithLock(attempt.deliveryId)
                ?: return NotificationDeliveryPreparation(NotificationDeliveryAction.INVALID)
        if (!delivery.hasClaim(
                attempt.claimToken,
                now,
            )
        ) {
            return NotificationDeliveryPreparation(NotificationDeliveryAction.RETRY, delaySeconds = 5)
        }
        val outcomes = result.outcomes.associateBy { it.token }
        val attempts = attempt.targets.associateBy { it.id }
        val targets = targetRepository.findAllByDeliveryIdOrderByIdAsc(delivery.id)
        targets.forEach { target ->
            val snapshot = attempts[target.id] ?: return@forEach
            val outcome =
                outcomes[snapshot.target.token]
                    ?: PushTargetOutcome(snapshot.target.token, PushTargetStatus.RETRYABLE, "FCM_RESULT_MISSING")
            target.record(outcome, now)
            if (outcome.status == PushTargetStatus.INVALID_TOKEN) {
                tokenRepository.deactivateIfUnchanged(snapshot.tokenId, delivery.userId, snapshot.registeredAt)
            }
        }
        delivery.finishAttempt(attempt.claimToken, now, targets, retryPolicy)
        return delivery.decision(now)
    }

    @Transactional
    fun extendLease(attempt: NotificationDeliveryAttempt): Boolean =
        deliveryRepository
            .findByIdWithLock(
                attempt.deliveryId,
            )?.extend(attempt.claimToken, LocalDateTime.now(clock), leaseSeconds)
            ?: false

    @Transactional
    fun recordDeadLetter(receipt: NotificationQueueReceipt) {
        if (incidentRepository.existsById(receipt.messageId)) return
        val message = receipt.message
        val delivery = message?.let { deliveryRepository.findByIdWithLock(it.deliveryId) }
        val reason =
            when {
                message == null -> "MALFORMED_MESSAGE"
                delivery == null -> "UNKNOWN_DELIVERY"
                delivery.eventId != message.eventId -> "EVENT_ID_MISMATCH"
                else -> "RECEIVE_LIMIT_EXCEEDED"
            }
        if (delivery != null && delivery.eventId == message?.eventId) delivery.markDeadLetter(LocalDateTime.now(clock))
        incidentRepository.save(NotificationQueueIncident.create(receipt, reason))
    }

    @Transactional
    fun requeueFailedDelivery(deliveryId: Long) {
        val delivery = checkNotNull(deliveryRepository.findByIdWithLock(deliveryId)) { "발송 작업이 없습니다." }
        val outbox = checkNotNull(outboxRepository.findByDeliveryIdWithLock(deliveryId)) { "Outbox가 없습니다." }
        val now = LocalDateTime.now(clock)
        delivery.requeue(now)
        outbox.requeue(now)
    }
}
