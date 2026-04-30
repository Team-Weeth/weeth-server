package com.weeth.domain.penalty.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class PenaltyByCardinalResponse(
    @field:Schema(description = "기수 번호", example = "4")
    val cardinal: Int?,
    @field:Schema(description = "해당 기수의 유저별 패널티 목록")
    val responses: List<PenaltyResponse>,
)
