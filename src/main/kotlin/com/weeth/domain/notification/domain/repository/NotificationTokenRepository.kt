package com.weeth.domain.notification.domain.repository

import com.weeth.domain.notification.domain.entity.NotificationToken
import com.weeth.domain.notification.domain.vo.PushTarget
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface NotificationTokenRepository :
    JpaRepository<NotificationToken, Long>,
    NotificationTokenReader {
    override fun findByToken(token: String): NotificationToken?

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        value = """
            INSERT INTO notification_token (
                user_id,
                token,
                platform,
                is_active,
                last_registered_at,
                created_at,
                modified_at
            )
            VALUES (
                :userId,
                :token,
                :platform,
                true,
                :registeredAt,
                :registeredAt,
                :registeredAt
            )
            ON DUPLICATE KEY UPDATE
                user_id = :userId,
                platform = :platform,
                is_active = true,
                last_registered_at = :registeredAt,
                modified_at = :registeredAt
        """,
        nativeQuery = true,
    )
    fun registerToken(
        @Param("userId") userId: Long,
        @Param("token") token: String,
        @Param("platform") platform: String,
        @Param("registeredAt") registeredAt: LocalDateTime,
    ): Int

    @Query(
        """
        SELECT new com.weeth.domain.notification.domain.vo.PushTarget(nt.token, nt.platform)
        FROM NotificationToken nt
        WHERE nt.user.id IN :userIds
        AND nt.isActive = true
        """,
    )
    override fun findActiveTargetsByUserIds(
        @Param("userIds") userIds: List<Long>,
    ): List<PushTarget>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE NotificationToken nt
        SET nt.isActive = false
        WHERE nt.token IN :invalidTokens
        AND nt.lastRegisteredAt <= :registeredBeforeOrAt
        """,
    )
    fun deactivateInvalidTokens(
        @Param("invalidTokens") invalidTokens: List<String>,
        @Param("registeredBeforeOrAt") registeredBeforeOrAt: LocalDateTime,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE NotificationToken nt
        SET nt.isActive = false
        WHERE nt.user.id = :userId
        AND nt.token = :token
        """,
    )
    fun deactivateByUserIdAndToken(
        @Param("userId") userId: Long,
        @Param("token") token: String,
    ): Int
}
