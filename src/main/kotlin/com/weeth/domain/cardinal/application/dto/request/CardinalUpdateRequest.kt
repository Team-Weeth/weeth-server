package com.weeth.domain.cardinal.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema

data class CardinalUpdateRequest(
    @field:Schema(description = "기수 ID", example = "1")
    val id: Long,
    @field:Schema(description = "현재 진행중 여부", example = "false")
    val inProgress: Boolean,
)
