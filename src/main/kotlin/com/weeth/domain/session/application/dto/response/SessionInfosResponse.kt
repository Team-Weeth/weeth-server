package com.weeth.domain.session.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class SessionInfosResponse(
    @field:Schema(description = "이번 주 정기모임 목록")
    val thisWeek: List<SessionInfoResponse>,
    @field:Schema(description = "정기모임 목록")
    val sessions: List<SessionGroupResponse>,
)
