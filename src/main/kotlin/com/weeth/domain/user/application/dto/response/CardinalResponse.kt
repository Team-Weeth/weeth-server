package com.weeth.domain.user.application.dto.response

import com.weeth.domain.user.domain.entity.enums.CardinalStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class CardinalResponse(
    @field:Schema(description = "기수 ID", example = "1")
    val id: Long,
    @field:Schema(description = "기수 번호", example = "7")
    val cardinalNumber: Int,
    @field:Schema(description = "년도", example = "2025", nullable = true)
    val year: Int?,
    @field:Schema(description = "학기", example = "1", nullable = true)
    val semester: Int?,
    @field:Schema(description = "기수 상태", example = "CURRENT")
    val status: CardinalStatus,
    @field:Schema(description = "생성 시각")
    val createdAt: LocalDateTime?,
    @field:Schema(description = "수정 시각")
    val modifiedAt: LocalDateTime?,
)
