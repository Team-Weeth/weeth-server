package com.weeth.domain.cardinal.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema

data class CardinalSaveRequest(
    @field:Schema(description = "기수", example = "4")
    val cardinalNumber: Int,
    @field:Schema(description = "현재 진행중 여부", example = "false")
    val inProgress: Boolean,
)
