package com.weeth.domain.attendance.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class QrStatusResponse(
    @field:Schema(description = "QR 활성 여부", example = "true")
    val isActive: Boolean,
    @field:Schema(description = "현재 활성 QR 세션 ID", example = "123")
    val currentSessionId: Long?,
    @field:Schema(description = "QR 만료 시각", example = "2026-05-13T10:05:00")
    val expiresAt: LocalDateTime?,
)
