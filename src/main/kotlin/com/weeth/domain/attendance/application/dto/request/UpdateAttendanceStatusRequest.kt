package com.weeth.domain.attendance.application.dto.request

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern

data class UpdateAttendanceStatusRequest(
    @field:NotNull
    val attendanceId: Long,
    @field:NotNull
    @field:Pattern(regexp = "ATTEND|ABSENT")
    val status: String,
)
