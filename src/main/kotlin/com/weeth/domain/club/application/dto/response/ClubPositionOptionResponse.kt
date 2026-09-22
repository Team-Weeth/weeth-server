package com.weeth.domain.club.application.dto.response

import com.weeth.domain.club.domain.enums.PositionColor
import io.swagger.v3.oas.annotations.media.Schema

data class ClubPositionOptionResponse(
    @field:Schema(description = "포지션 옵션 ID", example = "1")
    val id: Long,
    @field:Schema(description = "포지션 이름", example = "백엔드")
    val name: String,
    @field:Schema(description = "포지션 색상 프리셋", example = "PRIMARY")
    val color: PositionColor,
    @field:Schema(description = "표시 순서", example = "0")
    val displayOrder: Int,
)
