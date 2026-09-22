package com.weeth.domain.penalty.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

data class SavePenaltyRuleRequest(
    @field:Schema(description = "패널티 규정 내용 (null 또는 빈 값이면 삭제)", example = "정기 모임에 출석을 하지 않았을때 (=결석)", nullable = true)
    @field:Size(max = 500)
    val content: String?,
)
