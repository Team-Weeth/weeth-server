package com.weeth.domain.user.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class SignUpRequest(
    @field:Schema(description = "이름", example = "홍길동")
    @field:NotBlank
    val name: String,
    @field:Schema(description = "이메일", example = "hong@example.com")
    @field:Email
    @field:NotBlank
    val email: String,
    @field:Schema(description = "학번", example = "20201234")
    @field:NotBlank
    val studentId: String,
    @field:Schema(description = "전화번호", example = "01012345678")
    @field:NotBlank
    val tel: String,
    @field:Schema(description = "학과", example = "컴퓨터공학과")
    @field:NotNull
    val department: String,
    @field:Schema(description = "지원 기수", example = "7")
    @field:NotNull
    val cardinal: Int,
)
