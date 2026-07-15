package com.weeth.domain.user.application.dto.request

import com.weeth.domain.file.application.dto.request.FileSaveRequest
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateMultiProfileRequest(
    @field:Schema(description = "프로필 이름", example = "홍길동")
    @field:NotBlank
    @field:Size(max = 20)
    val name: String,
    @field:Schema(description = "프로필 사진")
    @field:Valid
    val profileImage: FileSaveRequest? = null,
    @field:Schema(description = "헤더 사진")
    @field:Valid
    val headerImage: FileSaveRequest? = null,
    @field:Schema(description = "자기소개", example = "안녕하세요!")
    @field:Size(max = 30)
    val bio: String? = null,
    @field:Schema(description = "생성 직후 사용할 동아리 ID 목록", example = "[\"1A2b3C\", \"4D5e6F\"]")
    val clubIds: List<String> = emptyList(),
)
