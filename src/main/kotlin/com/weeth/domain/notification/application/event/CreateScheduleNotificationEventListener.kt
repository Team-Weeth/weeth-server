package com.weeth.domain.notification.application.event

import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.repository.ClubMemberCardinalReader
import com.weeth.domain.notification.application.usecase.command.CreateNotificationUseCase
import com.weeth.domain.notification.domain.enums.NotificationReferenceType
import com.weeth.domain.notification.domain.enums.NotificationType
import com.weeth.domain.notification.domain.vo.NotificationRequest
import com.weeth.domain.schedule.application.event.EventCreatedEvent
import com.weeth.domain.session.application.event.SessionCreatedEvent
import com.weeth.global.common.id.TsidBase62Encoder
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.time.format.DateTimeFormatter

@Component
class CreateScheduleNotificationEventListener(
    private val memberCardinalReader: ClubMemberCardinalReader,
    private val createNotificationUseCase: CreateNotificationUseCase,
) {
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handle(event: EventCreatedEvent) {
        create(
            request(
                event.clubId,
                "EVENT_CREATED:${event.clubId}:${event.eventId}",
                NotificationType.EVENT_CREATED,
                NotificationReferenceType.EVENT,
                event.eventId,
                mapOf("scheduleTitle" to event.title, "startAt" to event.start.format(START_FORMAT)),
            ),
            event.cardinalNumber,
            event.authorUserId,
        )
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handle(event: SessionCreatedEvent) {
        val eventId =
            if (event.sessionGroupId == null) {
                "SESSION_CREATED:${event.clubId}:${event.sessionId}"
            } else {
                "SESSION_GROUP_CREATED:${event.clubId}:${event.sessionGroupId}"
            }
        create(
            request(
                event.clubId,
                eventId,
                NotificationType.SESSION_CREATED,
                NotificationReferenceType.SESSION,
                event.sessionId,
                mapOf(
                    "scheduleTitle" to event.title,
                    "startAt" to event.start.format(START_FORMAT),
                    "sessionCount" to event.sessionCount.toString(),
                ),
            ),
            event.cardinalNumber,
            event.authorUserId,
        )
    }

    private fun create(
        request: NotificationRequest,
        cardinalNumber: Int,
        authorUserId: Long,
    ) {
        val recipients =
            memberCardinalReader
                .findAllByClubIdAndCardinalNumber(request.clubId, cardinalNumber, MemberStatus.ACTIVE)
                .map { it.clubMember.user.id }
                .filter { it != authorUserId }
                .distinct()
        createNotificationUseCase.execute(request, recipients)
    }

    private fun request(
        clubId: Long,
        eventId: String,
        type: NotificationType,
        referenceType: NotificationReferenceType,
        referenceId: Long,
        variables: Map<String, String>,
    ): NotificationRequest {
        val encodedClubId = TsidBase62Encoder.encode(clubId)
        val targetPath = "/clubs/$encodedClubId/schedules"
        return NotificationRequest(
            eventId = eventId,
            type = type,
            variables = variables,
            targetPath = targetPath,
            clubId = clubId,
            referenceType = referenceType,
            referenceId = referenceId,
            data =
                mapOf(
                    "type" to type.name,
                    "referenceType" to referenceType.name,
                    "referenceId" to referenceId.toString(),
                    "clubId" to encodedClubId,
                    "targetPath" to targetPath,
                ),
        )
    }

    private companion object {
        val START_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
}
