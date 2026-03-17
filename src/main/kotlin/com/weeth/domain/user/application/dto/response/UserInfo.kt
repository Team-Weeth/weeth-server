package com.weeth.domain.user.application.dto.response

import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.enums.Role
import io.swagger.v3.oas.annotations.media.Schema

data class UserInfo(
    @field:Schema(description = "사용자 ID", example = "1")
    val id: Long,
    @field:Schema(description = "이름", example = "홍길동")
    val name: String,
    @field:Schema(description = "프로필 이미지 URL")
    val profileImageUrl: String?,
    @field:Schema(description = "권한", example = "USER")
    val role: Role?,
) {
    companion object {
        fun from(user: User) =
            UserInfo(
                id = user.id,
                name = user.name,
                profileImageUrl = user.profileImageUrl,
                role = user.role,
            )
    }
}
