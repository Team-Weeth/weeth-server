package com.weeth.domain.attendance.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema

data class CheckInRequest(
    @field:Schema(description = "출석 코드", example = "1234")
    val code: Int,
)
