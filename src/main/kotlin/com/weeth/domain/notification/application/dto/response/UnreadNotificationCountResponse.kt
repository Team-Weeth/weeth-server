package com.weeth.domain.notification.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class UnreadNotificationCountResponse(
    @field:Schema(description = "안 읽은 알림 개수", example = "3")
    val count: Long,
)
