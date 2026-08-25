package com.weeth.domain.schedule.application.dto.response

import com.weeth.domain.club.domain.enums.MemberRole
import io.swagger.v3.oas.annotations.media.Schema

data class AttendeeResponse(
    @field:Schema(description = "이름", example = "홍길동")
    val name: String,
    @field:Schema(description = "학과", example = "컴퓨터공학과")
    val department: String?,
    @field:Schema(description = "직급", example = "USER")
    val role: MemberRole,
    @field:Schema(description = "프로필 이미지 URL")
    val profileImageUrl: String?,
)
