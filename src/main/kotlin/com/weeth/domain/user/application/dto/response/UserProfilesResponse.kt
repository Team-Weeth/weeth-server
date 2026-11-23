package com.weeth.domain.user.application.dto.response

import com.weeth.domain.user.domain.enums.PlatformRole
import io.swagger.v3.oas.annotations.media.Schema

data class UserProfilesResponse(
    @field:Schema(description = "멀티프로필 목록")
    val profiles: List<UserProfileResponse>,
    @field:Schema(
        description = "플랫폼 전역 권한. DEVELOPER면 릴리즈 노트 백오피스 접근 가능, 일반 사용자는 null. UI 제어 용도이며 권한 검증은 서버가 수행한다.",
        example = "DEVELOPER",
        nullable = true,
    )
    val platformRole: PlatformRole? = null,
)
