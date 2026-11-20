package com.weeth.domain.notification.domain.vo

import com.weeth.domain.notification.domain.enums.NotificationTemplateFailureReason

sealed interface NotificationRenderingResult {
    data class Success(
        val message: RenderedNotification,
    ) : NotificationRenderingResult

    data class Failure(
        val reason: NotificationTemplateFailureReason,
        val missingVariables: Set<String> = emptySet(),
        val templateVersion: Int? = null,
    ) : NotificationRenderingResult
}
