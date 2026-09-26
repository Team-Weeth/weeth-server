package com.weeth.domain.notification.domain.repository

import com.weeth.domain.notification.domain.entity.UserNotification
import com.weeth.domain.notification.domain.enums.NotificationType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface UserNotificationReader {
    fun findTargetUserIdsByTypeAndPostId(
        type: NotificationType,
        postId: Long,
    ): List<Long>

    fun countByUserIdAndClubIdAndIsReadFalse(
        userId: Long,
        clubId: Long,
    ): Long

    fun findByUserIdAndClubIdAndId(
        userId: Long,
        clubId: Long,
        notificationId: Long,
    ): UserNotification?

    fun findAllByUserIdAndClubIdOrderByCreatedAtDesc(
        userId: Long,
        clubId: Long,
        pageable: Pageable,
    ): Page<UserNotification>
}
