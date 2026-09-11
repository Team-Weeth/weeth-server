package com.weeth.domain.club.application.dto.response

import com.weeth.domain.club.domain.enums.MemberRole
import io.swagger.v3.oas.annotations.media.Schema

data class ClubMemberPublicResponse(
    @field:Schema(description = "멤버 ID", example = "1")
    val clubMemberId: Long,
    @field:Schema(description = "사용자 이름", example = "홍길동")
    val name: String,
    @field:Schema(description = "프로필 이미지 URL", example = "https://cdn.weeth.com/profile/1.png", nullable = true)
    val profileImageUrl: String?,
    @field:Schema(description = "멤버 권한", example = "USER")
    val memberRole: MemberRole,
    @field:Schema(description = "소속 기수 목록", example = "[6, 7]")
    val cardinals: List<Int>,
    @field:Schema(description = "자기소개", example = "안녕하세요", nullable = true)
    val bio: String?,
)
