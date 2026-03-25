package com.weeth.domain.club.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class ProfileStatusResponse(
    @field:Schema(description = "기수 등록 여부")
    val cardinalAssigned: Boolean,
    @field:Schema(description = "프로필 완성 여부 (이름, 학번, 전화번호, 학교, 학과)")
    val profileCompleted: Boolean,
    @field:Schema(description = "미완성 필드 목록", example = "[\"studentId\", \"tel\"]")
    val missingFields: List<String>,
)
