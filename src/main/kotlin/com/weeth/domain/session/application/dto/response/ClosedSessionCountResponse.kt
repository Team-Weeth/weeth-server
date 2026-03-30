package com.weeth.domain.session.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class ClosedSessionCountResponse(
    @field:Schema(description = "이미 진행된(CLOSED) 세션 수")
    val closedSessionCount: Int,
)
