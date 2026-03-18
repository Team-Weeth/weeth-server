package com.weeth.domain.club.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class ClubPublicResponse(
    @field:Schema(description = "동아리 ID (Base62 인코딩)", example = "1A2b3C")
    val id: String,
    @field:Schema(description = "동아리 이름", example = "Leets")
    val name: String,
    @field:Schema(description = "동아리 소개", example = "함께 배우고 성장하는 개발자 커뮤니티")
    val description: String?,
    @field:Schema(description = "프로필 사진 URL")
    val profileImageUrl: String?,
)
