package com.weeth.domain.club.application.dto.request

import com.weeth.domain.club.domain.entity.ClubPositionOption
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size

data class SaveClubPositionOptionsRequest(
    @field:Schema(description = "포지션 옵션 목록 (관리자가 배치한 순서대로 저장, 최대 6개)")
    @field:Valid
    @field:Size(max = ClubPositionOption.MAX_OPTION_COUNT)
    val options: List<ClubPositionOptionRequest>,
)
