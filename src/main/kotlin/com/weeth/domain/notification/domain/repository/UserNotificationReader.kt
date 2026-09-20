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

    fun countByUserIdAndIsReadFalse(userId: Long): Long

    fun findByUserIdAndId(
        userId: Long,
        notificationId: Long,
    ): UserNotification?

    fun findAllByUserIdOrderByCreatedAtDesc(
        userId: Long,
        pageable: Pageable,
    ): Page<UserNotification>
}
