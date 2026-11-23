package com.weeth.domain.notification.fixture

import com.weeth.domain.notification.application.event.NotificationCreatedEvent
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.util.concurrent.CopyOnWriteArrayList

class NotificationEventProbe {
    val events = CopyOnWriteArrayList<NotificationCreatedEvent>()

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun record(event: NotificationCreatedEvent) {
        events += event
    }
}
