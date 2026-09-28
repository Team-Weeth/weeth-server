package com.weeth.domain.notification.application.event

import com.weeth.domain.board.application.event.NoticeCreatedEvent
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.notification.application.usecase.command.CreateNotificationUseCase
import com.weeth.domain.notification.domain.enums.NotificationReferenceType
import com.weeth.domain.notification.domain.enums.NotificationType
import com.weeth.domain.notification.domain.vo.NotificationContent
import com.weeth.global.common.id.TsidBase62Encoder
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class CreateNoticeNotificationEventListener(
    private val clubMemberReader: ClubMemberReader,
    private val createNotificationUseCase: CreateNotificationUseCase,
) {
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handle(event: NoticeCreatedEvent) {
        val targetUserIds =
            clubMemberReader.findActiveUserIdsByClubIdExcludingUserId(
                clubId = event.clubId,
                cardinalNumber = event.cardinalNumber,
                excludedUserId = event.authorUserId,
            )
        val encodedClubId = TsidBase62Encoder.encode(event.clubId)
        val targetPath = "/clubs/$encodedClubId/boards/${event.boardId}/posts/${event.postId}"
        val content =
            NotificationContent.create(
                type = NotificationType.NOTICE_CREATED,
                title = NOTICE_NOTIFICATION_TITLE,
                body = event.title,
                targetPath = targetPath,
                clubId = event.clubId,
                referenceType = NotificationReferenceType.POST,
                referenceId = event.postId,
                data =
                    mapOf(
                        "type" to NotificationType.NOTICE_CREATED.name,
                        "referenceType" to NotificationReferenceType.POST.name,
                        "referenceId" to event.postId.toString(),
                        "clubId" to encodedClubId,
                        "boardId" to event.boardId.toString(),
                        "targetPath" to targetPath,
                    ),
            )
        createNotificationUseCase.execute(content, targetUserIds)
    }

    private companion object {
        const val NOTICE_NOTIFICATION_TITLE = "새 공지가 등록되었습니다"
    }
}
