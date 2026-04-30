package com.weeth.domain.club.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class ClubJoinRequest(
    @field:Schema(description = "초대 코드", example = "550e8400-e29b-41d4-a716-446655440000")
    @field:NotBlank
    val code: String,
)
