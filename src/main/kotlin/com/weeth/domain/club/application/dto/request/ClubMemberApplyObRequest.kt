package com.weeth.domain.club.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema

data class ClubMemberApplyObRequest(
    @field:Schema(description = "대상 멤버 ID", example = "1")
    val clubMemberId: Long,
    @field:Schema(description = "적용할 기수", example = "8")
    val cardinal: Int,
)
