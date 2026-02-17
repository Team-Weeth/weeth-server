package com.weeth.domain.attendance.application.dto.response

import com.weeth.domain.attendance.domain.entity.enums.Status
import java.time.LocalDateTime

data class AttendanceResponse(
    val id: Long,
    val status: Status?,
    val title: String?,
    val start: LocalDateTime?,
    val end: LocalDateTime?,
    val location: String?,
)
