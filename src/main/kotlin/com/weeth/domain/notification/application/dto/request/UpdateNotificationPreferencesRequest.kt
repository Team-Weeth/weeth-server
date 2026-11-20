package com.weeth.domain.notification.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Size

data class UpdateNotificationPreferencesRequest(
    @field:Schema(description = "동아리 전체 알림 수신 여부 (null=변경 안 함)", nullable = true, example = "false")
    val masterEnabled: Boolean? = null,
    @field:Schema(description = "변경할 카테고리 설정 (null=변경 안 함, 목록 밖 설정 유지)", nullable = true)
    @field:Valid
    @field:Size(max = 4)
    val preferences: List<NotificationPreferenceItemRequest?>? = null,
) {
    @get:AssertTrue(message = "카테고리 설정 항목은 null일 수 없습니다.")
    @get:Schema(hidden = true)
    val isItemsPresent: Boolean get() = preferences?.all { it != null } ?: true
}
