package com.weeth.domain.notification.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class NotificationPreferencesResponse(
    @field:Schema(description = "동아리 전체 알림 수신 여부", example = "true")
    val masterEnabled: Boolean,
    @field:Schema(description = "미사용 카테고리도 포함한 전체 카테고리 설정")
    val preferences: List<NotificationCategoryPreferenceResponse>,
)
