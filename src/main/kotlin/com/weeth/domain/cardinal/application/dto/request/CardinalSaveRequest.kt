package com.weeth.domain.cardinal.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

data class CardinalSaveRequest(
    @field:Schema(description = "기수", example = "4")
    val cardinalNumber: Int,
    @field:Schema(description = "년도", example = "2024")
    val year: Int,
    @field:Schema(description = "학기", example = "2")
    val semester: Int,
    @field:Schema(description = "현재 진행중 여부", example = "false")
    val inProgress: Boolean,
)
