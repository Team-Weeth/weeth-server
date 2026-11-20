package com.weeth.domain.notification.domain.service

import com.weeth.domain.notification.domain.enums.NotificationCategory
import com.weeth.domain.notification.domain.enums.NotificationPreferenceUpdateFailure
import org.springframework.stereotype.Service

@Service
class NotificationPreferencePolicy {
    fun availableCategories(
        warningEnabled: Boolean,
        duesEnabled: Boolean,
    ): Set<NotificationCategory> =
        buildSet {
            add(NotificationCategory.NOTICE)
            add(NotificationCategory.SCHEDULE)
            if (duesEnabled) add(NotificationCategory.DUES)
            if (warningEnabled) add(NotificationCategory.WARNING)
        }

    fun validateUpdate(
        currentMasterEnabled: Boolean,
        requestedMasterEnabled: Boolean?,
        categories: List<NotificationCategory>,
        availableCategories: Set<NotificationCategory>,
    ): NotificationPreferenceUpdateFailure? =
        when {
            categories.distinct().size != categories.size -> {
                NotificationPreferenceUpdateFailure.DUPLICATE_CATEGORY
            }

            categories.isNotEmpty() && !(requestedMasterEnabled ?: currentMasterEnabled) -> {
                NotificationPreferenceUpdateFailure.MASTER_DISABLED
            }

            categories.any { it !in availableCategories } -> {
                NotificationPreferenceUpdateFailure.CATEGORY_UNAVAILABLE
            }

            else -> {
                null
            }
        }
}
