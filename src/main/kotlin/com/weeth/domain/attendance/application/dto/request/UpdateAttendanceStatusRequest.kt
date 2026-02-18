package com.weeth.domain.attendance.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern

data class UpdateAttendanceStatusRequest(
    @field:Schema(description = "출석 ID", example = "1")
    @field:NotNull
    val attendanceId: Long,
    @field:Schema(description = "변경할 출석 상태", example = "ATTEND")
    @field:NotNull
    @field:Pattern(regexp = "ATTEND|ABSENT")
    val status: String,
)
