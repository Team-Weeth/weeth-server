package com.weeth.domain.notification.application.event

import com.weeth.domain.notification.application.usecase.command.CreateNotificationUseCase
import com.weeth.domain.notification.domain.enums.NotificationReferenceType
import com.weeth.domain.notification.domain.enums.NotificationType
import com.weeth.domain.notification.domain.vo.NotificationRequest
import com.weeth.domain.penalty.application.event.WarningAssignedEvent
import com.weeth.global.common.id.TsidBase62Encoder
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class CreateWarningNotificationEventListener(
    private val createNotificationUseCase: CreateNotificationUseCase,
) {
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handle(event: WarningAssignedEvent) {
        val clubId = TsidBase62Encoder.encode(event.clubId)
        val targetPath = "/clubs/$clubId/penalties"
        createNotificationUseCase.execute(
            NotificationRequest(
                eventId = "WARNING_ASSIGNED:${event.clubId}:${event.penaltyId}",
                type = NotificationType.WARNING_ASSIGNED,
                variables = mapOf("warningReason" to event.reason),
                targetPath = targetPath,
                clubId = event.clubId,
                referenceType = NotificationReferenceType.PENALTY,
                referenceId = event.penaltyId,
                data =
                    mapOf(
                        "type" to NotificationType.WARNING_ASSIGNED.name,
                        "referenceType" to NotificationReferenceType.PENALTY.name,
                        "referenceId" to event.penaltyId.toString(),
                        "clubId" to clubId,
                        "targetPath" to targetPath,
                    ),
            ),
            listOf(event.userId),
        )
    }
}
