package com.weeth.domain.notification.application.event

import com.weeth.domain.account.application.event.DuesObligationCreatedEvent
import com.weeth.domain.notification.application.usecase.command.CreateNotificationUseCase
import com.weeth.domain.notification.domain.enums.NotificationReferenceType
import com.weeth.domain.notification.domain.enums.NotificationType
import com.weeth.domain.notification.domain.vo.NotificationRequest
import com.weeth.global.common.id.TsidBase62Encoder
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.text.NumberFormat
import java.util.Locale

@Component
class CreateDuesNotificationEventListener(
    private val createNotificationUseCase: CreateNotificationUseCase,
) {
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handle(event: DuesObligationCreatedEvent) {
        val clubId = TsidBase62Encoder.encode(event.clubId)
        val targetPath = "/clubs/$clubId/accounts"
        createNotificationUseCase.execute(
            NotificationRequest(
                eventId = "DUES_OBLIGATION_CREATED:${event.accountId}:${event.paymentTargetId}",
                type = NotificationType.DUES_OBLIGATION_CREATED,
                variables =
                    mapOf(
                        "accountName" to event.accountName,
                        "dueAmount" to NumberFormat.getIntegerInstance(Locale.KOREA).format(event.amount),
                    ),
                targetPath = targetPath,
                clubId = event.clubId,
                referenceType = NotificationReferenceType.ACCOUNT_PAYMENT_TARGET,
                referenceId = event.paymentTargetId,
                data =
                    mapOf(
                        "type" to NotificationType.DUES_OBLIGATION_CREATED.name,
                        "referenceType" to NotificationReferenceType.ACCOUNT_PAYMENT_TARGET.name,
                        "referenceId" to event.paymentTargetId.toString(),
                        "accountId" to event.accountId.toString(),
                        "clubId" to clubId,
                        "targetPath" to targetPath,
                    ),
            ),
            listOf(event.userId),
        )
    }
}
