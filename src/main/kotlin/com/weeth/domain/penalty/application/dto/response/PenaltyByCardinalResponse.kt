package com.weeth.domain.penalty.application.dto.response

data class PenaltyByCardinalResponse(
    val cardinal: Int?,
    val responses: List<PenaltyResponse>,
)
