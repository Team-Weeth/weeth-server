package com.weeth.domain.club.application.dto.request

import com.weeth.domain.club.domain.enums.MemberRole
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class ClubMemberRoleUpdateRequest(
    @field:Schema(description = "멤버 ID", example = "1")
    @field:Positive
    val clubMemberId: Long,
    @field:Schema(description = "변경할 권한", example = "ADMIN")
    val memberRole: MemberRole,
)
