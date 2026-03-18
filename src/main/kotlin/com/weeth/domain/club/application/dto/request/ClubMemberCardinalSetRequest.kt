package com.weeth.domain.club.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive

data class ClubMemberCardinalSetRequest(
    @field:Schema(description = "활동 기수 번호 목록", example = "[1, 2, 3]")
    @field:NotEmpty
    val cardinals: List<@Positive Int>,
)
