package com.weeth.domain.notification.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterNotificationTokenRequest(
    @field:Schema(description = "웹 FCM registration token", example = "fcm-registration-token")
    @field:NotBlank
    @field:Size(max = 500)
    val token: String,
)
