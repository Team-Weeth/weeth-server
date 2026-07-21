package com.weeth.domain.user.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class UserProfileAssignableClubsResponse(
    @field:Schema(description = "프로필을 사용할 수 있는 동아리 목록")
    val clubs: List<UserProfileAssignableClubResponse>,
)

data class UserProfileAssignableClubResponse(
    @field:Schema(description = "동아리 ID", example = "1A2b3C")
    val clubId: String,
    @field:Schema(description = "동아리 이름", example = "Leets")
    val name: String,
    @field:Schema(description = "동아리 사진 URL", nullable = true)
    val clubImage: String? = null,
    @field:Schema(description = "동아리 멤버 수", example = "100")
    val clubMemberNumber: Long,
)
