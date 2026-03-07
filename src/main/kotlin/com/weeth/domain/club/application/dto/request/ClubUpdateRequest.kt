package com.weeth.domain.club.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

data class ClubUpdateRequest(
    @field:Schema(description = "동아리 이름 (null=변경 안 함)", example = "Leets")
    @field:Size(max = 100)
    val name: String? = null,
    @field:Schema(description = "학교 이름 (null=변경 안 함)", example = "가천대학교")
    @field:Size(max = 50)
    val schoolName: String? = null,
    @field:Schema(description = "동아리 소개 (null=변경 안 함)", example = "함께 배우고 성장하는 개발자 커뮤니티")
    val description: String? = null,
    @field:Schema(description = "연락 이메일 (null=변경 안 함)", example = "club@example.com")
    val contactEmail: String? = null,
    @field:Schema(description = "연락 전화번호 (null=변경 안 함)", example = "010-1234-5678")
    val contactPhoneNumber: String? = null,
    @field:Schema(description = "프로필 사진 URL (null=변경 안 함)", example = "https://s3.amazonaws.com/bucket/profile.jpg")
    val profileImageUrl: String? = null,
    @field:Schema(description = "배경 사진 URL (null=변경 안 함)", example = "https://s3.amazonaws.com/bucket/background.jpg")
    val backgroundImageUrl: String? = null,
)
