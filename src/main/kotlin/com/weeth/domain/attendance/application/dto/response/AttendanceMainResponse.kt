package com.weeth.domain.attendance.application.dto.response

import com.weeth.domain.attendance.domain.entity.enums.Status
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class AttendanceMainResponse(
    val attendanceRate: Int?,
    val title: String?,
    val status: Status?,
    @field:Schema(description = "어드민인 경우 출석 코드 노출")
    val code: Int?,
    val start: LocalDateTime?,
    val end: LocalDateTime?,
    val location: String?,
)
