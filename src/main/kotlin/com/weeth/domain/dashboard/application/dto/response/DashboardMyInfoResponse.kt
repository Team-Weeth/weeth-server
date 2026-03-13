package com.weeth.domain.dashboard.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class DashboardMyInfoResponse(
    @field:Schema(description = "이름", example = "홍길동")
    val name: String,
    @field:Schema(description = "프로필 이미지 URL")
    val profileImageUrl: String?,
    @field:Schema(description = "자기소개")
    val bio: String?,
)
