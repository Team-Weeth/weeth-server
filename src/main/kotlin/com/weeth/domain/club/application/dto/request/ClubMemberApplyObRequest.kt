package com.weeth.domain.club.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Positive

data class ClubMemberApplyObRequest(
    @field:Schema(description = "대상 멤버 ID", example = "1")
    @field:Positive
    val clubMemberId: Long,
    @field:Schema(description = "적용할 기수", example = "8")
    @field:Positive
    val cardinal: Int,
)
