package com.weeth.domain.notification.domain.entity

import com.weeth.domain.notification.domain.enums.NotificationReferenceType
import com.weeth.domain.notification.domain.enums.NotificationType
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
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_notification",
    indexes = [
        Index(
            name = "idx_user_notification_user_club_created",
            columnList = "user_id, club_id, created_at DESC",
        ),
        Index(
            name = "idx_user_notification_user_club_read",
            columnList = "user_id, club_id, is_read",
        ),
        Index(
            name = "idx_user_notification_type_reference",
            columnList = "type, reference_type, reference_id",
        ),
    ],
)
class UserNotification(
    user: User,
    type: NotificationType,
    title: String,
    body: String,
    targetPath: String,
    clubId: Long,
    referenceType: NotificationReferenceType,
    referenceId: Long,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_notification_id")
    var id: Long = 0L
        private set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User = user
        private set

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    var type: NotificationType = type
        private set

    @Column(nullable = false, length = MAX_TITLE_LENGTH)
    var title: String = normalizeRequired(title, "알림 제목", MAX_TITLE_LENGTH)
        private set

    @Column(nullable = false, length = MAX_BODY_LENGTH)
    var body: String = normalizeDisplayText(body, "알림 내용", MAX_BODY_LENGTH)
        private set

    @Column(name = "target_path", nullable = false, length = MAX_TARGET_PATH_LENGTH)
    var targetPath: String = normalizeRequired(targetPath, "알림 이동 경로", MAX_TARGET_PATH_LENGTH)
        private set

    @Column(name = "club_id", nullable = false)
    var clubId: Long = clubId
        private set

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "reference_type", nullable = false, length = 30)
    var referenceType: NotificationReferenceType = referenceType
        private set

    @Column(name = "reference_id", nullable = false)
    var referenceId: Long = referenceId
        private set

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false
        private set

    @Column(name = "read_at")
    var readAt: LocalDateTime? = null
        private set

    fun markRead(now: LocalDateTime) {
        if (isRead) {
            return
        }
        isRead = true
        readAt = now
    }

    companion object {
        private const val MAX_TITLE_LENGTH = 100
        private const val MAX_BODY_LENGTH = 255
        private const val MAX_TARGET_PATH_LENGTH = 255

        fun create(
            user: User,
            type: NotificationType,
            title: String,
            body: String,
            targetPath: String,
            clubId: Long,
            referenceType: NotificationReferenceType,
            referenceId: Long,
        ): UserNotification =
            UserNotification(
                user = user,
                type = type,
                title = title,
                body = body,
                targetPath = targetPath,
                clubId = clubId,
                referenceType = referenceType,
                referenceId = referenceId,
            )

        private fun normalizeRequired(
            value: String,
            label: String,
            maxLength: Int,
        ): String {
            val normalized = value.trim()
            require(normalized.isNotBlank()) { "${label}은 공백일 수 없습니다." }
            require(normalized.length <= maxLength) { "${label}은 ${maxLength}자를 초과할 수 없습니다." }
            return normalized
        }

        private fun normalizeDisplayText(
            value: String,
            label: String,
            maxLength: Int,
        ): String {
            val normalized = value.trim()
            require(normalized.isNotBlank()) { "${label}은 공백일 수 없습니다." }
            return if (normalized.length <= maxLength) normalized else normalized.take(maxLength - 1) + "…"
        }
    }
}
