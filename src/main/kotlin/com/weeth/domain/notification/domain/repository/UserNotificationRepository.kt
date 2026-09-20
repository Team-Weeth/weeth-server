package com.weeth.domain.notification.domain.repository

import com.weeth.domain.notification.domain.entity.UserNotification
import com.weeth.domain.notification.domain.enums.NotificationType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface UserNotificationRepository :
    JpaRepository<UserNotification, Long>,
    UserNotificationReader {
    @Query(
        """
        SELECT un.user.id
        FROM UserNotification un
        WHERE un.type = :type
        AND un.postId = :postId
        """,
    )
    override fun findTargetUserIdsByTypeAndPostId(
        @Param("type") type: NotificationType,
        @Param("postId") postId: Long,
    ): List<Long>

    override fun countByUserIdAndIsReadFalse(userId: Long): Long

    override fun findByUserIdAndId(
        userId: Long,
        notificationId: Long,
    ): UserNotification?

    override fun findAllByUserIdOrderByCreatedAtDesc(
        userId: Long,
        pageable: Pageable,
    ): Page<UserNotification>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE UserNotification un
        SET un.isRead = true,
            un.readAt = :readAt
        WHERE un.user.id = :userId
        AND un.isRead = false
        """,
    )
    fun markAllReadByUserId(
        @Param("userId") userId: Long,
        @Param("readAt") readAt: LocalDateTime,
    ): Int
}
