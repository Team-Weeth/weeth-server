package com.weeth.domain.penalty.application.dto.response

import com.weeth.domain.club.domain.enums.MemberStatus
import io.swagger.v3.oas.annotations.media.Schema

data class MemberPenaltyDetailResponse(
    @field:Schema(description = "프로필 이미지 URL", nullable = true)
    val profileImageUrl: String?,
    @field:Schema(description = "이름", example = "홍길동")
    val name: String,
    @field:Schema(description = "소속 기수 목록", example = "[6, 7]")
    val cardinals: List<Int>,
    @field:Schema(description = "멤버 상태", example = "ACTIVE")
    val memberStatus: MemberStatus,
    @field:Schema(description = "자기소개", nullable = true)
    val bio: String?,
    @field:Schema(description = "페널티 목록")
    val penalties: List<PenaltyDetailResponse>,
)
