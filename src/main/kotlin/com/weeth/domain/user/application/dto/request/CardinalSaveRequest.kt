package com.weeth.domain.user.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

data class CardinalSaveRequest(
    @field:NotNull
    @field:Schema(description = "기수", example = "4")
    val cardinalNumber: Int,
    @field:NotNull
    @field:Schema(description = "년도", example = "2024")
    val year: Int,
    @field:NotNull
    @field:Schema(description = "학기", example = "2")
    val semester: Int,
    @field:NotNull
    @field:Schema(description = "현재 진행중 여부", example = "false")
    val inProgress: Boolean,
)
