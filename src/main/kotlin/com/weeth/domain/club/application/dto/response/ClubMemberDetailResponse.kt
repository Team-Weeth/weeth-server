package com.weeth.domain.club.application.dto.response

import com.weeth.domain.club.domain.enums.MemberRole
import io.swagger.v3.oas.annotations.media.Schema

data class ClubMemberDetailResponse(
    @field:Schema(description = "멤버 ID", example = "1")
    val clubMemberId: Long,
    @field:Schema(description = "사용자 이름", example = "홍길동")
    val name: String,
    @field:Schema(description = "프로필 이미지 URL", nullable = true)
    val profileImageUrl: String?,
    @field:Schema(description = "배경 이미지 URL", nullable = true)
    val headerImageUrl: String?,
    @field:Schema(description = "멤버 권한", example = "USER")
    val memberRole: MemberRole,
    @field:Schema(description = "소속 기수 목록", example = "[6, 7]")
    val cardinals: List<Int>,
    @field:Schema(description = "한줄소개", nullable = true)
    val bio: String?,
    @field:Schema(description = "전화번호", example = "01012345678", nullable = true)
    val tel: String?,
    @field:Schema(description = "이메일", example = "hong@example.com")
    val email: String,
    @field:Schema(description = "학번", example = "20201234", nullable = true)
    val studentId: String?,
    @field:Schema(description = "작성한 글 총 개수", example = "12")
    val postCount: Long,
)
