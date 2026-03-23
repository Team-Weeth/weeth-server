package com.weeth.domain.university.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class SchoolResponse(
    @field:Schema(description = "학교명", example = "가천대학교")
    val schoolName: String,
    @field:Schema(description = "지역", example = "경기도")
    val region: String,
)
