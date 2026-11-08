package com.weeth.domain.notification.application.usecase.command

import com.weeth.domain.notification.application.event.NotificationCreatedEvent
import com.weeth.domain.notification.domain.entity.NotificationDelivery
import com.weeth.domain.notification.domain.entity.NotificationOutbox
import com.weeth.domain.notification.domain.repository.NotificationDeliveryRepository
import com.weeth.domain.notification.domain.repository.NotificationOutboxRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class EnqueueNotificationDeliveryUseCase(
    private val deliveryRepository: NotificationDeliveryRepository,
    private val outboxRepository: NotificationOutboxRepository,
    private val clock: Clock,
) {
    @Transactional
    fun execute(event: NotificationCreatedEvent) {
        val now = LocalDateTime.now(clock)
        val deliveries =
            deliveryRepository.saveAll(
                event.targetUserIds.distinct().map { userId ->
                    NotificationDelivery.create(
                        event.eventId,
                        userId,
                        event.content.clubId,
                        event.content.type,
                        event.pushMessage,
                        event.content.data,
                        now,
                    )
                },
            )
        outboxRepository.saveAll(deliveries.map { NotificationOutbox.create(it.id, it.eventId, now) })
    }
}
