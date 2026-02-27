package com.weeth.domain.user.application.dto.response

import com.weeth.domain.user.domain.entity.enums.Role
import io.swagger.v3.oas.annotations.media.Schema

data class UserInfoResponse(
    @field:Schema(description = "사용자 ID", example = "1")
    val id: Long,
    @field:Schema(description = "이름", example = "홍길동")
    val name: String,
    @field:Schema(description = "소속 기수 목록", example = "[6, 7]")
    val cardinals: List<Int>,
    @field:Schema(description = "권한", example = "USER", nullable = true)
    val role: Role?,
)
