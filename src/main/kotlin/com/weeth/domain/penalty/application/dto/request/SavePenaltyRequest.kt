package com.weeth.domain.penalty.application.dto.request

import com.weeth.domain.penalty.domain.enums.PenaltyType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive

data class SavePenaltyRequest(
    @field:Schema(description = "페널티 대상 사용자 ID 목록", example = "[1, 2, 3]")
    @field:NotEmpty
    val userIds: List<Long>,
    @field:Schema(description = "페널티 점수", example = "1")
    @field:Positive
    val score: Int = 1,
    @field:Schema(description = "페널티 사유", example = "정기모임 무단 불참")
    @field:NotBlank
    val penaltyDescription: String,
    @field:Schema(description = "페널티 타입 (기본값: PENALTY)", example = "PENALTY", allowableValues = ["PENALTY", "WARNING"])
    val penaltyType: PenaltyType = PenaltyType.PENALTY,
)
