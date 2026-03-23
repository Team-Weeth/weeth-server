package com.weeth.domain.user.application.dto.response

import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.user.domain.entity.User
import io.swagger.v3.oas.annotations.media.Schema

data class UserInfo(
    @field:Schema(description = "사용자 ID", example = "1")
    val id: Long,
    @field:Schema(description = "이름", example = "홍길동")
    val name: String,
    @field:Schema(description = "프로필 이미지 URL")
    val profileImageUrl: String?,
    @field:Schema(description = "동아리 내 권한", example = "USER")
    val role: MemberRole,
) {
    companion object {
        fun from(
            user: User,
            clubMember: ClubMember,
        ) = UserInfo(
            id = user.id,
            name = user.name,
            profileImageUrl = clubMember.profileImageUrl,
            role = clubMember.memberRole,
        )
    }
}
