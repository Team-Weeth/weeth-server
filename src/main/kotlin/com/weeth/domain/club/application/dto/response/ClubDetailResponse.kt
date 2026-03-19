package com.weeth.domain.club.application.dto.response

import com.weeth.domain.club.domain.enums.PrimaryContact
import io.swagger.v3.oas.annotations.media.Schema

data class ClubDetailResponse(
    @field:Schema(description = "동아리 ID (Base62 인코딩)", example = "1A2b3C")
    val id: String,
    @field:Schema(description = "동아리 이름", example = "Leets")
    val name: String,
    @field:Schema(description = "초대 코드", example = "550e8400-e29b-41d4-a716-446655440000")
    val code: String,
    @field:Schema(description = "학교 이름", example = "가천대학교")
    val schoolName: String,
    @field:Schema(description = "동아리 소개", example = "함께 배우고 성장하는 개발자 커뮤니티")
    val description: String?,
    @field:Schema(description = "연락 이메일", example = "club@example.com")
    val contactEmail: String?,
    @field:Schema(description = "연락 전화번호", example = "010-1234-5678")
    val contactPhoneNumber: String?,
    @field:Schema(description = "주 연락처", example = "PHONE")
    val primaryContact: PrimaryContact,
    @field:Schema(description = "프로필 사진 URL")
    val profileImageUrl: String?,
    @field:Schema(description = "배경 사진 URL")
    val backgroundImageUrl: String?,
)
