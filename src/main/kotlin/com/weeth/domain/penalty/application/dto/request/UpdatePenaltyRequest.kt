package com.weeth.domain.penalty.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema

data class UpdatePenaltyRequest(
    @field:Schema(description = "수정할 패널티 ID", example = "1")
    val penaltyId: Long,
    @field:Schema(description = "수정할 패널티 사유", example = "정기모임 무단 불참 (수정)")
    val penaltyDescription: String?,
)
