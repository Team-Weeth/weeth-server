package com.weeth.domain.user.application.dto.request

import com.weeth.domain.file.application.dto.request.FileSaveRequest
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size

data class UpdateMultiProfileRequest(
    @field:Schema(description = "프로필 이름 (null=변경 안 함)", example = "새 이름", nullable = true)
    @field:Size(min = 1, max = 20)
    val name: String? = null,
    @field:Schema(description = "프로필 사진 (null=변경 안 함)", nullable = true)
    @field:Valid
    val profileImage: FileSaveRequest? = null,
    @field:Schema(description = "헤더 사진 (null=변경 안 함)", nullable = true)
    @field:Valid
    val headerImage: FileSaveRequest? = null,
    @field:Schema(description = "프로필 사진 삭제 여부 (true=삭제, null/false=삭제 안 함)", example = "false", nullable = true)
    val removeProfileImage: Boolean? = null,
    @field:Schema(description = "헤더 사진 삭제 여부 (true=삭제, null/false=삭제 안 함)", example = "false", nullable = true)
    val removeHeaderImage: Boolean? = null,
    @field:Schema(description = "자기소개 (null=변경 안 함)", example = "안녕하세요!", nullable = true)
    @field:Size(max = 30)
    val bio: String? = null,
)
