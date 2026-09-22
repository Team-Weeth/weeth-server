package com.weeth.domain.notification.application.event

import com.weeth.domain.board.application.event.NoticeCreatedEvent
import com.weeth.domain.notification.application.usecase.command.CreateNoticeNotificationUseCase
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class CreateNoticeNotificationEventListener(
    private val createNoticeNotificationUseCase: CreateNoticeNotificationUseCase,
) {
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handle(event: NoticeCreatedEvent) {
        createNoticeNotificationUseCase.execute(event)
    }
}
