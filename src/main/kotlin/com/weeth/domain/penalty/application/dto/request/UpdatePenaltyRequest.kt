package com.weeth.domain.penalty.application.dto.request

import jakarta.validation.constraints.NotNull

data class UpdatePenaltyRequest(
    @field:NotNull
    val penaltyId: Long,
    val penaltyDescription: String?,
)
