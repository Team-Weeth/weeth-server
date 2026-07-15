package com.weeth.domain.club.application.dto.response

import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.enums.MemberStatus
import io.swagger.v3.oas.annotations.media.Schema

data class ClubInfoResponse(
    @field:Schema(description = "동아리 ID (Base62 인코딩)", example = "1A2b3C")
    val id: String,
    @field:Schema(description = "동아리 이름", example = "Leets")
    val name: String,
    @field:Schema(description = "학교 이름", example = "가천대학교")
    val schoolName: String,
    @field:Schema(description = "동아리 설명", example = "함께 배우고 성장하는 개발자 커뮤니티")
    val description: String?,
    @field:Schema(description = "동아리 프로필 이미지 URL")
    val profileImageUrl: String?,
    @field:Schema(description = "활동 부원 수", example = "368")
    val memberCount: Long,
    @field:Schema(description = "활동 기수 목록", example = "[31, 32]")
    val cardinals: List<Int>,
    @field:Schema(description = "나의 권한", example = "USER")
    val memberRole: MemberRole,
    @field:Schema(description = "나의 멤버 상태", example = "ACTIVE")
    val memberStatus: MemberStatus,
    @field:Schema(description = "현재 사용 중인 멀티프로필")
    val usingProfile: ClubUsingProfileResponse? = null,
)

data class ClubUsingProfileResponse(
    @field:Schema(description = "프로필 ID", example = "1")
    val profileId: Long,
    @field:Schema(description = "프로필 이름", example = "길동")
    val name: String,
    @field:Schema(description = "프로필 이미지 URL")
    val profileImageUrl: String? = null,
    @field:Schema(description = "자기소개", example = "안녕하세요")
    val bio: String? = null,
)
