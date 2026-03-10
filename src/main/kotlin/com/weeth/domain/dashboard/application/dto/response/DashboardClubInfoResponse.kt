package com.weeth.domain.dashboard.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class DashboardClubInfoResponse(
    @field:Schema(description = "동아리 ID (Base62)", example = "1A2b3C")
    val id: String,
    @field:Schema(description = "동아리 이름", example = "Leets")
    val name: String,
    @field:Schema(description = "학교 이름", example = "가천대학교")
    val schoolName: String,
    @field:Schema(description = "동아리 설명", example = "IT 동아리")
    val description: String?,
    @field:Schema(description = "활성 멤버 수", example = "70")
    val memberCount: Long,
    @field:Schema(description = "프로필 이미지 URL")
    val profileImageUrl: String?,
    @field:Schema(description = "배경 이미지 URL")
    val backgroundImageUrl: String?,
)
