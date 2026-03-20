package com.weeth.domain.club.application.dto.request

import com.weeth.domain.file.application.dto.request.FileSaveRequest
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size

data class UpdateMemberProfileRequest(
    @field:Schema(description = "프로필 사진")
    @field:Valid
    val profileImage: FileSaveRequest? = null,

    @field:Schema(description = "자기소개", example = "안녕하세요!")
    @field:Size(max = 30)
    val bio: String? = null,
)
