package com.weeth.domain.club.application.dto.request

import com.weeth.domain.club.domain.enums.PrimaryContact
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class ClubCreateRequest(
    @field:Schema(description = "동아리 이름", example = "Leets")
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,
    @field:Schema(description = "학교 이름", example = "가천대학교")
    @field:NotBlank
    @field:Size(max = 50)
    val schoolName: String,
    @field:Schema(description = "동아리 소개", example = "함께 배우고 성장하는 개발자 커뮤니티")
    @field:Size(max = 30)
    val description: String? = null,
    @field:Schema(description = "연락 이메일", example = "club@example.com")
    @field:Email
    val contactEmail: String? = null,
    @field:Schema(description = "연락 전화번호", example = "01012345678")
    @field:NotBlank
    val contactPhoneNumber: String,
    @field:Schema(description = "주 연락처", example = "PHONE")
    val primaryContact: PrimaryContact,
    @field:Schema(description = "가장 최근 기수 번호", example = "7")
    @field:Positive
    val currentCardinal: Int,
    @field:Schema(description = "프로필 사진 S3 URL", example = "https://s3.amazonaws.com/bucket/profile.jpg")
    val profileImageUrl: String? = null,
    @field:Schema(description = "배경 사진 S3 URL", example = "https://s3.amazonaws.com/bucket/background.jpg")
    val backgroundImageUrl: String? = null,
)
