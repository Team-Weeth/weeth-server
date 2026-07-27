package com.weeth.domain.user.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class UserProfileClubResponse(
    @field:Schema(description = "동아리 ID", example = "1A2b3C")
    val clubId: String,
    @field:Schema(description = "동아리 이름", example = "Leets")
    val name: String,
)
