package com.weeth.domain.schedule.application.dto.response

data class SessionInfosResponse(
    val thisWeek: SessionInfoResponse?,
    val meetings: List<SessionInfoResponse>,
)
