package com.weeth.domain.session.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class SessionUpdateRequest(
    @field:Schema(description = "세션 제목 (null=변경 안 함)", example = "1차 정기모임")
    val title: String?,
    @field:Schema(description = "세션 내용 (null=변경 안 함)", example = "OT 및 자기소개")
    @field:Size(max = 500)
    val content: String?,
    @field:Schema(description = "모임 장소 (null=변경 안 함)", example = "공학관 401호")
    val location: String?,
    @field:Schema(description = "시작 시간 (null=변경 안 함)", example = "2026-03-27T10:00:00")
    val start: LocalDateTime?,
    @field:Schema(description = "종료 시간 (null=변경 안 함)", example = "2026-03-27T22:00:00")
    val end: LocalDateTime?,
)
