package com.weeth.domain.penalty.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class UpdatePenaltyRequest(
    @field:Schema(description = "수정할 페널티 ID", example = "1")
    val penaltyId: Long,
    @field:Schema(description = "수정할 페널티 사유 (null=변경 안 함)", example = "정기모임 무단 불참 (수정)", nullable = true)
    @field:Size(max = 20)
    val penaltyDescription: String?,
    @field:Schema(description = "수정할 페널티 점수 (null=변경 안 함)", example = "2", nullable = true)
    @field:Positive
    val score: Int?,
)
