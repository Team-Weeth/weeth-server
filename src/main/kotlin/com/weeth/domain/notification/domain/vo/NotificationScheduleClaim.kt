package com.weeth.domain.notification.domain.vo

import java.time.LocalDate

data class NotificationScheduleClaim(
    val runId: Long,
    val clubId: Long,
    val localDate: LocalDate,
    val token: String,
)
