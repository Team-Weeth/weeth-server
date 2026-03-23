package com.weeth.domain.club.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class ClubMembershipStatusResponse(
    @field:Schema(description = "ACTIVE 상태 동아리 존재 여부", example = "true")
    val hasActiveClub: Boolean,
    @field:Schema(description = "WAITING 상태 동아리 존재 여부", example = "false")
    val hasWaitingClub: Boolean,
    @field:Schema(description = "ACTIVE 동아리 정보 (없으면 null)")
    val activeClub: ClubInfoResponse?,
    @field:Schema(description = "WAITING 동아리 정보 (없으면 null)")
    val waitingClub: ClubInfoResponse?,
)
