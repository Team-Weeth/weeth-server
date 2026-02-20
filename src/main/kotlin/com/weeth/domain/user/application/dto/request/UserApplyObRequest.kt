package com.weeth.domain.user.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

data class UserApplyObRequest(
    @field:Schema(description = "대상 사용자 ID", example = "1")
    @field:NotNull
    val userId: Long,
    @field:Schema(description = "적용할 기수", example = "8")
    @field:NotNull
    val cardinal: Int,
)
