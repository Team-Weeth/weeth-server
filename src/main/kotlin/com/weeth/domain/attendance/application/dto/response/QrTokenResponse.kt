package com.weeth.domain.attendance.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class QrTokenResponse(
    @field:Schema(description = "6자리 출석 코드", example = "123456")
    val code: Int,
    @field:Schema(description = "QR 만료 시각", example = "2025-03-02T10:30:00")
    val expiredAt: LocalDateTime,
)
