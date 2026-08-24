package com.weeth.domain.penalty.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class PenaltyDetailResponse(
    @field:Schema(description = "페널티 ID", example = "1")
    val penaltyId: Long,
    @field:Schema(description = "기수 번호", example = "4")
    val cardinal: Int?,
    @field:Schema(description = "페널티 사유", example = "정기모임 무단 불참")
    val penaltyDescription: String,
    @field:Schema(description = "페널티 점수", example = "1")
    val score: Int,
    @field:Schema(description = "페널티 부여 일시", example = "2026-02-19T01:00:00")
    val time: LocalDateTime,
)
