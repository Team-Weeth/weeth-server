package com.weeth.domain.notification.application.usecase.command

import com.weeth.domain.notification.application.event.NotificationCreatedEvent
import com.weeth.domain.notification.domain.entity.NotificationFailure
import com.weeth.domain.notification.domain.entity.UserNotification
import com.weeth.domain.notification.domain.enums.NotificationChannel
import com.weeth.domain.notification.domain.port.UserNotificationBulkWriter
import com.weeth.domain.notification.domain.repository.NotificationFailureRepository
import com.weeth.domain.notification.domain.service.NotificationRecipientPolicy
import com.weeth.domain.notification.domain.service.NotificationTemplateRenderer
import com.weeth.domain.notification.domain.vo.NotificationContent
import com.weeth.domain.notification.domain.vo.NotificationRenderingResult
import com.weeth.domain.notification.domain.vo.NotificationRequest
import com.weeth.domain.notification.domain.vo.RenderedNotification
import com.weeth.domain.user.domain.repository.UserReader
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateNotificationUseCase(
    private val userReader: UserReader,
    private val userNotificationBulkWriter: UserNotificationBulkWriter,
    private val eventPublisher: ApplicationEventPublisher,
    private val recipientPolicy: NotificationRecipientPolicy,
    private val templateRenderer: NotificationTemplateRenderer,
    private val failureRepository: NotificationFailureRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun execute(
        request: NotificationRequest,
        targetUserIds: List<Long>,
    ) {
        if (targetUserIds.isEmpty()) return
        val eligibleIds = recipientPolicy.filterEligibleUserIds(request.clubId, request.type.category, targetUserIds)
        if (eligibleIds.isEmpty()) return
        val users = userReader.findAllByIds(eligibleIds)
        if (users.isEmpty()) return

        val messages = mutableMapOf<NotificationChannel, RenderedNotification>()
        for (channel in NotificationChannel.entries) {
            when (val result = templateRenderer.render(request.type, channel, request.variables)) {
                is NotificationRenderingResult.Success -> {
                    messages[channel] = result.message
                }

                is NotificationRenderingResult.Failure -> {
                    failureRepository.save(NotificationFailure.create(request, channel, result))
                    log.warn(
                        "알림 템플릿 렌더링 실패. eventId={}, type={}, channel={}, version={}, reason={}, missingVariables={}",
                        request.eventId,
                        request.type,
                        channel,
                        result.templateVersion,
                        result.reason,
                        result.missingVariables,
                    )
                    return
                }
            }
        }
        val app = messages.getValue(NotificationChannel.IN_APP)
        val content =
            NotificationContent.create(
                type = request.type,
                title = app.title,
                body = app.body,
                targetPath = request.targetPath,
                clubId = request.clubId,
                referenceType = request.referenceType,
                referenceId = request.referenceId,
                data = request.data.toMap(),
                templateVersion = app.templateVersion,
            )
        val notifications =
            users.map { user ->
                UserNotification.create(
                    user = user,
                    type = content.type,
                    title = content.title,
                    body = content.body,
                    targetPath = content.targetPath,
                    clubId = content.clubId,
                    referenceType = content.referenceType,
                    referenceId = content.referenceId,
                    templateVersion = app.templateVersion,
                )
            }
        userNotificationBulkWriter.saveAll(notifications)
        eventPublisher.publishEvent(
            NotificationCreatedEvent(
                content = content,
                targetUserIds = users.map { it.id },
                pushMessage = messages.getValue(NotificationChannel.PUSH),
                eventId = request.eventId,
            ),
        )
    }
}
