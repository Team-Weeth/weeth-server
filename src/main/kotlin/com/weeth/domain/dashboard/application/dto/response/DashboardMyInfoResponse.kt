package com.weeth.domain.dashboard.application.dto.response

import com.weeth.domain.user.application.dto.response.UserInfo
import io.swagger.v3.oas.annotations.media.Schema

data class DashboardMyInfoResponse(
    @field:Schema(description = "사용자 정보")
    val userInfo: UserInfo,
    @field:Schema(description = "동아리 프로필 이미지 URL")
    val profileImageUrl: String?,
    @field:Schema(description = "자기소개")
    val bio: String?,
)
