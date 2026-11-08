package com.weeth.domain.notification.domain.repository

import com.weeth.domain.notification.domain.entity.NotificationSetting
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationSettingRepository : JpaRepository<NotificationSetting, Long> {
    fun findByUserIdAndClubId(
        userId: Long,
        clubId: Long,
    ): NotificationSetting?

    fun findAllByClubIdAndUserIdIn(
        clubId: Long,
        userIds: List<Long>,
    ): List<NotificationSetting>
}
