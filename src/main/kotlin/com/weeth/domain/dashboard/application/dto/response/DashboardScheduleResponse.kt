package com.weeth.domain.dashboard.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class DashboardScheduleResponse(
    @field:Schema(description = "일정 ID", example = "1")
    val id: Long,
    @field:Schema(description = "일정 제목", example = "Spring 스터디")
    val title: String,
    @field:Schema(description = "시작 일시", example = "2026-03-09T14:00:00")
    val start: LocalDateTime,
    @field:Schema(description = "종료 일시", example = "2026-03-09T16:00:00")
    val end: LocalDateTime,
    @field:Schema(description = "정기 모임 여부 (true=세션, false=일반 일정)", example = "false")
    val isMeeting: Boolean,
)
