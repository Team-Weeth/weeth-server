package com.weeth.domain.user.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class SocialLoginRequest(
    @field:Schema(description = "OAuth2 인가 코드(auth code)", example = "SplxlOBeZQQYbYS6WxSbIA")
    @field:NotBlank
    val authCode: String,
    @field:Schema(description = "추가 입력 이름(선택)", example = "홍길동", nullable = true)
    val name: String? = null,
    @field:Schema(description = "추가 입력 학번(선택)", example = "20201234", nullable = true)
    val studentId: String? = null,
    @field:Schema(description = "추가 입력 전화번호(선택)", example = "01012345678", nullable = true)
    val tel: String? = null,
    @field:Schema(description = "추가 입력 학과(선택)", example = "컴퓨터공학과", nullable = true)
    val department: String? = null,
)
