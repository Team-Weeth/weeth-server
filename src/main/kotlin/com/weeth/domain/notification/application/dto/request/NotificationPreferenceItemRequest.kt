package com.weeth.domain.notification.application.dto.request

import com.weeth.domain.notification.domain.enums.NotificationCategory
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

/** PATCH 목록의 각 항목은 카테고리와 변경할 값을 함께 지정한다. */
data class NotificationPreferenceItemRequest(
    @field:Schema(description = "변경할 알림 카테고리", example = "NOTICE", requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotNull
    val category: NotificationCategory? = null,
    @field:Schema(description = "카테고리 수신 여부", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotNull
    val enabled: Boolean? = null,
)
