package com.weeth.domain.attendance.application.event

import java.time.LocalDateTime

data class AttendanceOpenEvent(
    val expiredAt: LocalDateTime,
)
