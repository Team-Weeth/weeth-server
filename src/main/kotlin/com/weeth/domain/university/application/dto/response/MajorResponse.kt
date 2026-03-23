package com.weeth.domain.university.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class MajorResponse(
    @field:Schema(description = "학과명", example = "컴퓨터공학과")
    val majorName: String,
    @field:Schema(description = "계열", example = "공학계열")
    val category: String,
)
