package com.weeth.domain.notification.presentation

import com.weeth.domain.notification.application.dto.request.RegisterNotificationTokenRequest
import com.weeth.domain.notification.application.dto.request.RevokeNotificationTokenRequest
import com.weeth.domain.notification.application.usecase.command.ManageNotificationTokenUseCase
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.auth.jwt.application.exception.JwtErrorCode
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "NOTIFICATION", description = "알림 API")
@RestController
@RequestMapping("/api/v4/notifications")
@ApiErrorCodeExample(JwtErrorCode::class)
class NotificationController(
    private val manageNotificationTokenUseCase: ManageNotificationTokenUseCase,
) {
    @PostMapping("/tokens")
    @Operation(summary = "웹 알림 토큰 등록")
    fun registerToken(
        @Valid @RequestBody request: RegisterNotificationTokenRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void?> {
        manageNotificationTokenUseCase.register(userId, request)
        return CommonResponse.success(NotificationResponseCode.NOTIFICATION_TOKEN_REGISTERED_SUCCESS)
    }

    @PostMapping("/tokens/revoke")
    @Operation(summary = "웹 알림 토큰 해제")
    fun revokeToken(
        @Valid @RequestBody request: RevokeNotificationTokenRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void?> {
        manageNotificationTokenUseCase.revoke(userId, request)
        return CommonResponse.success(NotificationResponseCode.NOTIFICATION_TOKEN_REVOKED_SUCCESS)
    }
}
