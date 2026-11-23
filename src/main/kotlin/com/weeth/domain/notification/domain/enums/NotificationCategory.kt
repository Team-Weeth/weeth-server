package com.weeth.domain.notification.domain.enums

enum class NotificationCategory {
    NOTICE,
    SCHEDULE,
    DUES,
    WARNING,
    ;

    val defaultEnabled: Boolean get() = true

    fun isEffectivelyEnabled(
        masterEnabled: Boolean,
        enabled: Boolean,
        available: Boolean,
    ): Boolean = masterEnabled && enabled && available
}
