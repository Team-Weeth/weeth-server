package com.weeth.domain.user.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class UserMyPageResponse(
    @field:Schema(description = "사용자 기본 개인정보")
    val user: UserMyPageInfoResponse,
    @field:Schema(description = "마이페이지 요약")
    val stats: UserMyPageStatsResponse,
    @field:Schema(description = "현재 사용 중인 프로필 목록")
    val usingProfiles: List<UserMyPageUsingProfileResponse>,
    @field:Schema(description = "현재 동아리에서 사용 중인 멀티프로필", nullable = true)
    val currentProfile: UserMyPageCurrentProfileResponse? = null,
)

data class UserMyPageInfoResponse(
    @field:Schema(description = "사용자 이름", example = "홍길동")
    val name: String,
    @field:Schema(description = "전화번호", example = "01012345678", nullable = true)
    val tel: String? = null,
    @field:Schema(description = "이메일", example = "hong@example.com")
    val email: String,
    @field:Schema(description = "학교", example = "가천대학교", nullable = true)
    val school: String? = null,
    @field:Schema(description = "학과", example = "컴퓨터공학과", nullable = true)
    val department: String? = null,
    @field:Schema(description = "학번", example = "20201234", nullable = true)
    val studentId: String? = null,
)

data class UserMyPageStatsResponse(
    @field:Schema(description = "작성한 게시글 수", example = "12")
    val postCount: Long,
    @field:Schema(description = "출석한 세션 수", example = "8")
    val attendedSessionCount: Long,
)

data class UserMyPageUsingProfileResponse(
    @field:Schema(description = "프로필 ID", example = "1")
    val profileId: Long,
    @field:Schema(description = "프로필 이름", example = "길동")
    val name: String,
    @field:Schema(description = "프로필 이미지 URL", nullable = true)
    val profileImageUrl: String? = null,
    @field:Schema(description = "헤더 이미지 URL", nullable = true)
    val headerImageUrl: String? = null,
    @field:Schema(description = "자기소개", nullable = true)
    val bio: String? = null,
    @field:Schema(description = "이 프로필을 사용 중인 동아리 목록")
    val clubs: List<UserProfileClubResponse>,
)

data class UserMyPageCurrentProfileResponse(
    @field:Schema(description = "멀티프로필 ID", example = "1")
    val profileId: Long,
    @field:Schema(description = "멀티 프로필 이름", example = "멀티 프로필 길동")
    val name: String,
    @field:Schema(description = "멀티 프로필 이미지 URL", nullable = true)
    val profileImageUrl: String? = null,
    @field:Schema(description = "멀티 프로필 헤더 이미지 URL", nullable = true)
    val headerImageUrl: String? = null,
    @field:Schema(description = "멀티 프로필 자기소개", nullable = true)
    val bio: String? = null,
)
