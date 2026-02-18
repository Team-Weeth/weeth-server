package com.weeth.domain.penalty.application.dto.response

data class PenaltyResponse(
    val userId: Long,
    val name: String,
    val penaltyCount: Int,
    val warningCount: Int,
    val cardinals: List<Int>,
    val penalties: List<PenaltyDetailResponse>,
)
