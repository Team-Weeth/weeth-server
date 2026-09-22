package com.weeth.domain.penalty.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class PenaltyRuleResponse(
    @field:Schema(description = "패널티 규정 내용 (미설정 시 null)", nullable = true)
    val content: String?,
)
