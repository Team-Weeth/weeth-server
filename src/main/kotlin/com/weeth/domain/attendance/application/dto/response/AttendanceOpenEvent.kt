package com.weeth.domain.attendance.application.dto.response

import java.time.LocalDateTime

data class AttendanceOpenEvent(
    val expiredAt: LocalDateTime,
)
