package com.weeth.domain.club.application.dto.request

import com.weeth.domain.club.domain.enums.PositionColor
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class ClubPositionOptionRequest(
    @field:Schema(description = "포지션 옵션 ID (null=신규 생성, 값이 있으면 해당 id의 기존 옵션을 수정)", example = "1", nullable = true)
    val id: Long?,
    @field:Schema(description = "포지션 이름 (최대 10자)", example = "백엔드")
    @field:NotBlank
    @field:Size(max = 10)
    val name: String,
    @field:Schema(description = "포지션 색상 프리셋", example = "PRIMARY")
    @field:NotNull
    val color: PositionColor,
)
