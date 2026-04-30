package com.weeth.domain.user.application.dto.request

import com.weeth.domain.club.domain.enums.MemberRole
import io.swagger.v3.oas.annotations.media.Schema

data class UserRoleUpdateRequest(
    @field:Schema(description = "대상 사용자 ID", example = "1")
    val userId: Long,
    @field:Schema(description = "변경할 동아리 내 권한", example = "ADMIN")
    val role: MemberRole,
)
