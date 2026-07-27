package com.weeth.domain.user.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class UserProfileResponse(
    @field:Schema(description = "프로필 ID", example = "1")
    val profileId: Long,
    @field:Schema(description = "프로필 이름", example = "길동")
    val name: String,
    @field:Schema(description = "프로필 이미지 URL", nullable = true)
    val profileImageUrl: String? = null,
    @field:Schema(description = "헤더 이미지 URL", nullable = true)
    val headerImageUrl: String? = null,
    @field:Schema(description = "자기소개", nullable = true)
    val bio: String? = null,
    @field:Schema(description = "사용 중인 동아리 목록")
    val usingClubs: List<UserProfileClubResponse> = emptyList(),
)
