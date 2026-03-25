package com.weeth.domain.penalty.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema

data class SavePenaltyRequest(
    @field:Schema(description = "패널티 대상 사용자 ID", example = "1")
    val userId: Long,
    @field:Schema(description = "패널티 사유", example = "정기모임 무단 불참")
    val penaltyDescription: String?,
)
