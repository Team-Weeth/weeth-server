package com.weeth.domain.user.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

data class UserIdsRequest(
    @field:Schema(description = "처리 대상 사용자 ID 목록", example = "[1, 2, 3]")
    @field:NotNull
    @field:NotEmpty
    val userId: List<Long>,
)
