package com.weeth.domain.user.application.dto.request

import com.weeth.domain.user.domain.entity.enums.Role
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

data class UserRoleUpdateRequest(
    @field:Schema(description = "대상 사용자 ID", example = "1")
    @field:NotNull
    val userId: Long,
    @field:Schema(description = "변경할 권한", example = "ADMIN")
    @field:NotNull
    val role: Role,
)
