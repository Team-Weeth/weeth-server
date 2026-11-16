package com.weeth.domain.notification.domain.vo

import com.weeth.domain.notification.domain.enums.NotificationReferenceType
import com.weeth.domain.notification.domain.enums.NotificationType

data class NotificationContent private constructor(
    val type: NotificationType,
    val title: String,
    val body: String,
    val targetPath: String,
    val clubId: Long,
    val referenceType: NotificationReferenceType,
    val referenceId: Long,
    val data: Map<String, String>,
) {
    companion object {
        fun create(
            type: NotificationType,
            title: String,
            body: String,
            targetPath: String,
            clubId: Long,
            referenceType: NotificationReferenceType,
            referenceId: Long,
            data: Map<String, String>,
        ): NotificationContent =
            NotificationContent(
                type = type,
                title = title,
                body = body.trim().truncate(MAX_BODY_LENGTH, "알림 내용"),
                targetPath = targetPath,
                clubId = clubId,
                referenceType = referenceType,
                referenceId = referenceId,
                data = data,
            )

        private const val MAX_BODY_LENGTH = 255

        private fun String.truncate(
            maxLength: Int,
            fieldName: String,
        ): String {
            require(isNotBlank()) { "$fieldName 은(는) 비어 있을 수 없습니다." }
            return if (length <= maxLength) this else take(maxLength - 1) + "…"
        }
    }
}
