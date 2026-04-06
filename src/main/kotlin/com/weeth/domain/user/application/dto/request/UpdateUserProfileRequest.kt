package com.weeth.domain.user.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateUserProfileRequest(
    @field:Schema(description = "이름", example = "홍길동")
    @field:Size(max = 20)
    val name: String? = null,
    @field:Schema(description = "이메일", example = "hong@example.com")
    @field:Email
    val email: String? = null,
    @field:Schema(description = "학번", example = "20201234")
    val studentId: String? = null,
    @field:Schema(description = "전화번호", example = "01012345678")
    val tel: String? = null,
    @field:Schema(description = "학교", example = "가천대학교")
    val school: String? = null,
    @field:Schema(description = "학과", example = "컴퓨터공학과")
    val department: String? = null,
)
