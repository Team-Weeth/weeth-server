package com.weeth.domain.notification.domain.entity

import com.weeth.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "notification_setting",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_notification_setting_user_club",
            columnNames = ["user_id", "club_id"],
        ),
    ],
)
class NotificationSetting private constructor(
    userId: Long,
    clubId: Long,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_setting_id")
    var id: Long = 0L
        private set

    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        private set

    @Column(name = "club_id", nullable = false)
    var clubId: Long = clubId
        private set

    @Column(name = "master_enabled", nullable = false)
    var masterEnabled: Boolean = true
        private set

    fun updateMaster(enabled: Boolean?) {
        enabled?.let { masterEnabled = it }
    }

    companion object {
        fun create(
            userId: Long,
            clubId: Long,
        ): NotificationSetting {
            require(userId > 0 && clubId > 0) { "사용자와 동아리 ID는 양수여야 합니다." }
            return NotificationSetting(userId, clubId)
        }
    }
}
