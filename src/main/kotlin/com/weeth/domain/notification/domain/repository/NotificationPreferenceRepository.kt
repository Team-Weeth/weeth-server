package com.weeth.domain.notification.domain.repository

import com.weeth.domain.notification.domain.entity.NotificationPreference
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationPreferenceRepository : JpaRepository<NotificationPreference, Long> {
    fun findAllByUserIdAndClubId(
        userId: Long,
        clubId: Long,
    ): List<NotificationPreference>
}
