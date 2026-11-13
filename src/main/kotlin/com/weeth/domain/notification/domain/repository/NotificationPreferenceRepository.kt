package com.weeth.domain.notification.domain.repository

import com.weeth.domain.notification.domain.entity.NotificationPreference
import com.weeth.domain.notification.domain.enums.NotificationCategory
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationPreferenceRepository : JpaRepository<NotificationPreference, Long> {
    fun findAllByUserIdAndClubId(
        userId: Long,
        clubId: Long,
    ): List<NotificationPreference>

    fun findAllByClubIdAndCategoryAndUserIdIn(
        clubId: Long,
        category: NotificationCategory,
        userIds: List<Long>,
    ): List<NotificationPreference>
}
