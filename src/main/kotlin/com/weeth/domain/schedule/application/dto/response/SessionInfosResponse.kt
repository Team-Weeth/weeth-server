package com.weeth.domain.schedule.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class SessionInfosResponse(
    @field:Schema(description = "이번 주 정기모임")
    val thisWeek: SessionInfoResponse?,
    @field:Schema(description = "정기모임 목록")
    val meetings: List<SessionInfoResponse>,
)
