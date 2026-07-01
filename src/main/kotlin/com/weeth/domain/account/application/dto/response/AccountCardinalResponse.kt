package com.weeth.domain.account.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class AccountCardinalResponse(
    @field:Schema(description = "회비 기수", example = "7")
    val cardinal: Int,
    @field:Schema(description = "회비 장부 이름", example = "7기 회비", nullable = true)
    val name: String?,
    @field:Schema(description = "선택 가능한 최신 기수 여부", example = "true")
    val isLatest: Boolean,
)
