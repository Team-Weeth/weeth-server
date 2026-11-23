package com.weeth.domain.notification.domain.enums

enum class NotificationType(
    val category: NotificationCategory,
) {
    NOTICE_CREATED(NotificationCategory.NOTICE),
    EVENT_CREATED(NotificationCategory.SCHEDULE),
    SESSION_CREATED(NotificationCategory.SCHEDULE),
    SESSION_DAY_REMINDER(NotificationCategory.SCHEDULE),
    DUES_OBLIGATION_CREATED(NotificationCategory.DUES),
}
