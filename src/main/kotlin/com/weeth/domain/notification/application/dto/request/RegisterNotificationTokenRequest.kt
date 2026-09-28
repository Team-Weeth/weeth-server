package com.weeth.domain.notification.application.dto.request

import com.weeth.domain.notification.domain.enums.NotificationPlatform
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterNotificationTokenRequest(
    @field:Schema(description = "FCM registration token", example = "fcm-registration-token")
    @field:NotBlank
    @field:Size(max = 500)
    val token: String,
    @field:Schema(description = "푸시 플랫폼", example = "WEB", defaultValue = "WEB")
    val platform: NotificationPlatform = NotificationPlatform.WEB,
)
