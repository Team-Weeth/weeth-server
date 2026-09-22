package com.weeth.domain.notification.application.usecase.command

import com.weeth.domain.board.application.event.NoticeCreatedEvent
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.notification.domain.entity.UserNotification
import com.weeth.domain.notification.domain.enums.NotificationType
import com.weeth.domain.notification.domain.repository.UserNotificationRepository
import com.weeth.domain.user.domain.repository.UserReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateNoticeNotificationUseCase(
    private val clubMemberReader: ClubMemberReader,
    private val userReader: UserReader,
    private val userNotificationRepository: UserNotificationRepository,
) {
    @Transactional
    fun execute(event: NoticeCreatedEvent) {
        val targetUserIds =
            clubMemberReader.findActiveUserIdsByClubIdExcludingUserId(
                clubId = event.clubId,
                excludedUserId = event.authorUserId,
            )
        if (targetUserIds.isEmpty()) {
            return
        }

        val users = userReader.findAllByIds(targetUserIds)
        val notifications =
            users.map {
                UserNotification.create(
                    user = it,
                    type = NotificationType.NOTICE_CREATED,
                    title = NOTICE_NOTIFICATION_TITLE,
                    body = event.title,
                    targetPath = createTargetPath(event),
                    clubId = event.clubId,
                    boardId = event.boardId,
                    postId = event.postId,
                )
            }

        userNotificationRepository.saveAll(notifications)
    }

    private fun createTargetPath(event: NoticeCreatedEvent): String =
        "/clubs/${event.clubId}/boards/${event.boardId}/posts/${event.postId}"

    private companion object {
        const val NOTICE_NOTIFICATION_TITLE = "새 공지가 등록되었습니다"
    }
}
