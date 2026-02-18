package com.weeth.domain.penalty.application.dto.request

import com.weeth.domain.penalty.domain.enums.PenaltyType
import jakarta.validation.constraints.NotNull

data class SavePenaltyRequest(
    @field:NotNull
    val userId: Long,
    @field:NotNull
    val penaltyType: PenaltyType,
    val penaltyDescription: String?,
)
