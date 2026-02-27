package com.weeth.domain.schedule.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class SessionInfoResponse(
    @field:Schema(description = "정기모임 ID", example = "1")
    val id: Long,
    @field:Schema(description = "기수", example = "4")
    val cardinal: Int,
    @field:Schema(description = "제목", example = "1차 정기모임")
    val title: String,
    @field:Schema(description = "시작 시간")
    val start: LocalDateTime,
)
