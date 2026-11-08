package com.weeth.domain.notification.application.dto.response

import com.weeth.domain.notification.domain.enums.NotificationCategory
import io.swagger.v3.oas.annotations.media.Schema

data class NotificationCategoryPreferenceResponse(
    @field:Schema(description = "알림 카테고리", example = "NOTICE")
    val category: NotificationCategory,
    @field:Schema(description = "저장된 선택값 또는 미설정 시 기본값. 마스터 OFF에도 보존", example = "true")
    val enabled: Boolean,
    @field:Schema(description = "미설정 시 기본 수신 여부", example = "true")
    val defaultEnabled: Boolean,
    @field:Schema(description = "동아리 기능 사용 여부. false면 UI에서 숨김", example = "true")
    val available: Boolean,
    @field:Schema(description = "마스터·선택값·기능 사용 여부를 모두 반영한 유효 수신 여부", example = "true")
    val effectiveEnabled: Boolean,
)
