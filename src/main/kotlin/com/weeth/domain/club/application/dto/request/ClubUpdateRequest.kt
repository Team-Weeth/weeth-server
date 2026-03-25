package com.weeth.domain.club.application.dto.request

import com.weeth.domain.club.domain.enums.PrimaryContact
import com.weeth.domain.file.application.dto.request.FileSaveRequest
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size

data class ClubUpdateRequest(
    @field:Schema(description = "동아리 이름 (null=변경 안 함)", example = "Leets")
    @field:Size(max = 100)
    val name: String? = null,
    @field:Schema(description = "학교 이름 (null=변경 안 함)", example = "가천대학교")
    @field:Size(max = 50)
    val schoolName: String? = null,
    @field:Schema(description = "동아리 소개 (null=변경 안 함)", example = "함께 배우고 성장하는 개발자 커뮤니티")
    @field:Size(max = 30)
    val description: String? = null,
    @field:Schema(description = "연락 이메일 (null=변경 안 함)", example = "club@example.com")
    @field:Email
    val contactEmail: String? = null,
    @field:Schema(description = "연락 전화번호 (null=변경 안 함)", example = "01012345678")
    @field:Size(min = 1)
    val contactPhoneNumber: String? = null,
    @field:Schema(description = "주 연락처 (null=변경 안 함)", example = "PHONE")
    val primaryContact: PrimaryContact? = null,
    @field:Schema(description = "프로필 사진 (null=변경 안 함)")
    @field:Valid
    val profileImage: FileSaveRequest? = null,
    @field:Schema(description = "배경 사진 (null=변경 안 함)")
    @field:Valid
    val backgroundImage: FileSaveRequest? = null,
)
