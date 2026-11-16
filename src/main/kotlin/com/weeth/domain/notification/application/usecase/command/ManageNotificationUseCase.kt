package com.weeth.domain.notification.application.usecase.command

import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.notification.application.exception.NotificationNotFoundException
import com.weeth.domain.notification.domain.repository.UserNotificationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ManageNotificationUseCase(
    private val clubMemberPolicy: ClubMemberPolicy,
    private val userNotificationRepository: UserNotificationRepository,
) {
    @Transactional
    fun markRead(
        clubId: Long,
        userId: Long,
        notificationId: Long,
    ) {
        clubMemberPolicy.getActiveMember(clubId, userId)
        val notification =
            userNotificationRepository.findByUserIdAndClubIdAndId(userId, clubId, notificationId)
                ?: throw NotificationNotFoundException()

        notification.markRead(LocalDateTime.now())
    }

    @Transactional
    fun markAllRead(
        clubId: Long,
        userId: Long,
    ) {
        clubMemberPolicy.getActiveMember(clubId, userId)
        userNotificationRepository.markAllReadByUserIdAndClubId(userId, clubId, LocalDateTime.now())
    }
}
