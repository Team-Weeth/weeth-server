package com.weeth.domain.penalty.application.dto.response

data class PenaltyResponse(
    val userId: Long?,
    val penaltyCount: Int?,
    val warningCount: Int?,
    val name: String?,
    val cardinals: List<Int>,
    val penalties: List<PenaltyDetailResponse>,
)
