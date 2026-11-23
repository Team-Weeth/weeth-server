package com.weeth.domain.notification.application.mapper

import com.weeth.domain.notification.application.dto.response.NotificationCategoryPreferenceResponse
import com.weeth.domain.notification.application.dto.response.NotificationPreferencesResponse
import com.weeth.domain.notification.domain.entity.NotificationPreference
import com.weeth.domain.notification.domain.enums.NotificationCategory
import org.springframework.stereotype.Component

@Component
class NotificationPreferenceMapper {
    fun toResponse(
        masterEnabled: Boolean,
        preferences: List<NotificationPreference>,
        availableCategories: Set<NotificationCategory>,
    ): NotificationPreferencesResponse {
        val preferencesByCategory = preferences.associateBy { it.category }
        return NotificationPreferencesResponse(
            masterEnabled = masterEnabled,
            preferences =
                NotificationCategory.entries.map { category ->
                    val enabled = preferencesByCategory[category]?.enabled ?: category.defaultEnabled
                    val available = category in availableCategories
                    NotificationCategoryPreferenceResponse(
                        category,
                        enabled,
                        category.defaultEnabled,
                        available,
                        category.isEffectivelyEnabled(masterEnabled, enabled, available),
                    )
                },
        )
    }
}
