package com.weeth.domain.penalty.application.dto.response

import com.weeth.domain.penalty.domain.enums.PenaltyType
import java.time.LocalDateTime

data class PenaltyDetailResponse(
    val penaltyId: Long?,
    val penaltyType: PenaltyType?,
    val cardinal: Int?,
    val penaltyDescription: String?,
    val time: LocalDateTime?,
)
