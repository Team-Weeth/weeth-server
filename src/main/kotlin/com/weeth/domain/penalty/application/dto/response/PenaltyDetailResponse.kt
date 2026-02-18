package com.weeth.domain.penalty.application.dto.response

import com.weeth.domain.penalty.domain.enums.PenaltyType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class PenaltyDetailResponse(
    @field:Schema(description = "패널티 ID", example = "1")
    val penaltyId: Long,
    @field:Schema(description = "패널티 유형", example = "WARNING")
    val penaltyType: PenaltyType,
    @field:Schema(description = "기수 번호", example = "4")
    val cardinal: Int?,
    @field:Schema(description = "패널티 사유", example = "정기모임 무단 불참")
    val penaltyDescription: String,
    @field:Schema(description = "최종 수정 시간", example = "2026-02-19T01:00:00")
    val time: LocalDateTime,
)
