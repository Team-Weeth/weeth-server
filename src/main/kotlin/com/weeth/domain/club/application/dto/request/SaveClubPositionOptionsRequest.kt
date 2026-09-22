package com.weeth.domain.club.application.dto.request

import com.weeth.domain.club.domain.entity.ClubPositionOption
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size

data class SaveClubPositionOptionsRequest(
    @field:Schema(description = "추가/수정할 포지션 옵션 목록 (id 없으면 신규 생성, id 있으면 해당 옵션 수정. 배치 순서대로 저장, 최대 6개)")
    @field:Valid
    @field:Size(max = ClubPositionOption.MAX_OPTION_COUNT)
    val options: List<ClubPositionOptionRequest>,
    @field:Schema(description = "삭제할 포지션 옵션 ID 목록")
    val deletedPositionIds: List<Long> = emptyList(),
)
