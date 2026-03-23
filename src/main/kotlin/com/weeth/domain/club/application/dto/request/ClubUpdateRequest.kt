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
    // TODO: FileSaveRequest로 전환 (ClubMember 프로필과 동일 패턴)
    @field:Size(max = 500)
    @field:Schema(description = "프로필 사진 storageKey (null=변경 안 함)", example = "CLUB_PROFILE/2026-02/uuid_profile.png")
    val profileImageStorageKey: String? = null,
    // TODO: FileSaveRequest로 전환 (ClubMember 프로필과 동일 패턴)
    @field:Size(max = 500)
    @field:Schema(
        description = "배경 사진 storageKey (null=변경 안 함)",
        example = "CLUB_BACKGROUND/2026-02/uuid_background.png",
    )
    val backgroundImageStorageKey: String? = null,
)
