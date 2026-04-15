package com.weeth.domain.club.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class ClubCreateResponse(
    @field:Schema(description = "동아리 ID (Base62 인코딩)", example = "YUNJcjFKMO")
    val clubId: String,
    @field:Schema(description = "동아리 이름", example = "Leets")
    val clubName: String,
)
