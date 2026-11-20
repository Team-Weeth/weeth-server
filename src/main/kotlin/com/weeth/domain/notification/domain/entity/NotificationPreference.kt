package com.weeth.domain.notification.domain.entity

import com.weeth.domain.notification.domain.enums.NotificationCategory
import com.weeth.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(
    name = "notification_preference",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_notification_preference_user_club_category",
            columnNames = ["user_id", "club_id", "category"],
        ),
    ],
)
class NotificationPreference private constructor(
    userId: Long,
    clubId: Long,
    category: NotificationCategory,
    enabled: Boolean,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_preference_id")
    var id: Long = 0L
        private set

    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        private set

    @Column(name = "club_id", nullable = false)
    var clubId: Long = clubId
        private set

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    var category: NotificationCategory = category
        private set

    @Column(nullable = false)
    var enabled: Boolean = enabled
        private set

    fun updateEnabled(enabled: Boolean?) {
        enabled?.let { this.enabled = it }
    }

    companion object {
        fun create(
            userId: Long,
            clubId: Long,
            category: NotificationCategory,
            enabled: Boolean,
        ): NotificationPreference {
            require(userId > 0 && clubId > 0) { "사용자와 동아리 ID는 양수여야 합니다." }
            return NotificationPreference(userId, clubId, category, enabled)
        }
    }
}
