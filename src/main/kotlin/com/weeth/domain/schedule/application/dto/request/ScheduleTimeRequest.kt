package com.weeth.domain.schedule.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDateTime

data class ScheduleTimeRequest(
    @field:Schema(description = "시작 시간", example = "2024-03-01T10:00:00")
    @field:NotNull
    @field:DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    val start: LocalDateTime,
    @field:Schema(description = "종료 시간", example = "2024-03-01T12:00:00")
    @field:NotNull
    @field:DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    val end: LocalDateTime,
)
