package com.weeth.domain.schedule.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class ScheduleUpdateRequest(
    @field:Schema(description = "일정 제목 (null=변경 안 함)", example = "MT")
    val title: String?,
    @field:Schema(description = "일정 내용 (null=변경 안 함)", example = "1박 2일 MT입니다.")
    @field:Size(max = 500)
    val content: String?,
    @field:Schema(description = "장소 (null=변경 안 함)", example = "가평")
    val location: String?,
    @field:Schema(description = "시작 시간 (null=변경 안 함)", example = "2026-03-28T10:00:00")
    val start: LocalDateTime?,
    @field:Schema(description = "종료 시간 (null=변경 안 함)", example = "2026-03-28T10:00:00")
    val end: LocalDateTime?,
)
