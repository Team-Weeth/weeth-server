package com.weeth.domain.penalty.application.event

data class WarningAssignedEvent(
    val clubId: Long,
    val penaltyId: Long,
    val userId: Long,
    val reason: String,
)
