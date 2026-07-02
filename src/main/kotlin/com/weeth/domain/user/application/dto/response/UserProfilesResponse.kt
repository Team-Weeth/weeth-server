package com.weeth.domain.user.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class UserProfilesResponse(
    @field:Schema(description = "멀티프로필 목록")
    val profiles: List<UserProfileResponse>,
)
