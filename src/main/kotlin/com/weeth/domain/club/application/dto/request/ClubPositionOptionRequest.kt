package com.weeth.domain.club.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class ClubPositionOptionRequest(
    @field:Schema(description = "포지션 이름 (최대 10자)", example = "백엔드")
    @field:NotBlank
    @field:Size(max = 10)
    val name: String,
    @field:Schema(description = "포지션 색상 hex 값 (#RRGGBB)", example = "#4CAF50")
    @field:NotBlank
    @field:Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "색상은 #RRGGBB 형식의 hex 값이어야 합니다.")
    val colorHex: String,
)
