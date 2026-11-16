package com.weeth.domain.notification.domain.entity

import com.weeth.domain.notification.domain.enums.NotificationPlatform
import com.weeth.domain.user.domain.entity.User
import com.weeth.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(
    name = "notification_token",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_notification_token_token",
            columnNames = ["token"],
        ),
    ],
    indexes = [
        Index(
            name = "idx_notification_token_user_active",
            columnList = "user_id, is_active",
        ),
    ],
)
class NotificationToken(
    user: User,
    token: String,
    platform: NotificationPlatform,
    registeredAt: LocalDateTime,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_token_id")
    var id: Long = 0L
        private set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User = user
        private set

    @Column(nullable = false, length = MAX_TOKEN_LENGTH)
    var token: String = normalizeToken(token)
        private set

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    var platform: NotificationPlatform = platform
        private set

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
        private set

    @Column(name = "last_registered_at", nullable = false)
    var lastRegisteredAt: LocalDateTime = registeredAt
        private set

    fun reactivate(
        user: User,
        platform: NotificationPlatform,
        registeredAt: LocalDateTime,
    ) {
        this.user = user
        this.platform = platform
        isActive = true
        lastRegisteredAt = registeredAt
    }

    fun deactivate() {
        isActive = false
    }

    companion object {
        private const val MAX_TOKEN_LENGTH = 500

        fun create(
            user: User,
            token: String,
            platform: NotificationPlatform,
            registeredAt: LocalDateTime,
        ): NotificationToken =
            NotificationToken(
                user = user,
                token = token,
                platform = platform,
                registeredAt = registeredAt,
            )

        fun normalizeToken(token: String): String {
            val normalized = token.trim()
            require(normalized.isNotBlank()) { "알림 토큰은 공백일 수 없습니다." }
            require(normalized.length <= MAX_TOKEN_LENGTH) { "알림 토큰은 ${MAX_TOKEN_LENGTH}자를 초과할 수 없습니다." }
            return normalized
        }
    }
}
