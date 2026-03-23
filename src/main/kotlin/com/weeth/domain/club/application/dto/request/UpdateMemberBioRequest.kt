package com.weeth.domain.club.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

data class UpdateMemberBioRequest(
    @field:Schema(description = "자기소개", example = "안녕하세요!")
    @field:Size(max = 30)
    val bio: String?,
)
