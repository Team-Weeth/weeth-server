package com.weeth.domain.club.application.dto.response

import com.weeth.domain.club.domain.enums.MemberRole
import io.swagger.v3.oas.annotations.media.Schema

data class ClubMemberSummaryResponse(
    @field:Schema(description = "사용자 ID", example = "1")
    val userId: Long,
    @field:Schema(description = "이름", example = "홍길동")
    val name: String,
    @field:Schema(description = "소속 기수 목록", example = "[6, 7]")
    val cardinals: List<Int>,
    @field:Schema(description = "동아리 내 권한", example = "USER")
    val role: MemberRole,
)
