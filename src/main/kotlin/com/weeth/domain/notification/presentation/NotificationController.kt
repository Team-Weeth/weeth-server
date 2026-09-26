package com.weeth.domain.notification.presentation

import com.weeth.domain.club.application.exception.ClubErrorCode
import com.weeth.domain.notification.application.dto.request.RegisterNotificationTokenRequest
import com.weeth.domain.notification.application.dto.request.RevokeNotificationTokenRequest
import com.weeth.domain.notification.application.dto.response.NotificationResponse
import com.weeth.domain.notification.application.dto.response.UnreadNotificationCountResponse
import com.weeth.domain.notification.application.exception.NotificationErrorCode
import com.weeth.domain.notification.application.usecase.command.ManageNotificationTokenUseCase
import com.weeth.domain.notification.application.usecase.command.ManageNotificationUseCase
import com.weeth.domain.notification.application.usecase.query.GetNotificationQueryService
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.auth.jwt.application.exception.JwtErrorCode
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import com.weeth.global.common.response.PageResponse
import com.weeth.global.common.web.TsidParam
import com.weeth.global.common.web.TsidPathVariable
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "NOTIFICATION", description = "알림 API")
@RestController
@RequestMapping("/api/v4")
@ApiErrorCodeExample(NotificationErrorCode::class, ClubErrorCode::class, JwtErrorCode::class)
class NotificationController(
    private val manageNotificationTokenUseCase: ManageNotificationTokenUseCase,
    private val manageNotificationUseCase: ManageNotificationUseCase,
    private val getNotificationQueryService: GetNotificationQueryService,
) {
    @PostMapping("/notifications/tokens")
    @Operation(summary = "웹 알림 토큰 등록")
    fun registerToken(
        @Valid @RequestBody request: RegisterNotificationTokenRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void?> {
        manageNotificationTokenUseCase.register(userId, request)
        return CommonResponse.success(NotificationResponseCode.NOTIFICATION_TOKEN_REGISTERED_SUCCESS)
    }

    @PostMapping("/notifications/tokens/revoke")
    @Operation(summary = "웹 알림 토큰 해제")
    fun revokeToken(
        @Valid @RequestBody request: RevokeNotificationTokenRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void?> {
        manageNotificationTokenUseCase.revoke(userId, request)
        return CommonResponse.success(NotificationResponseCode.NOTIFICATION_TOKEN_REVOKED_SUCCESS)
    }

    @GetMapping("/clubs/{clubId}/notifications/unread-count")
    @Operation(summary = "안 읽은 알림 개수 조회")
    fun countUnread(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<UnreadNotificationCountResponse> =
        CommonResponse.success(
            NotificationResponseCode.NOTIFICATION_UNREAD_COUNT_SUCCESS,
            getNotificationQueryService.countUnread(clubId = clubId, userId = userId),
        )

    @GetMapping("/clubs/{clubId}/notifications")
    @Operation(summary = "알림 목록 조회")
    fun findNotifications(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<PageResponse<NotificationResponse>> =
        CommonResponse.success(
            NotificationResponseCode.NOTIFICATION_LIST_SUCCESS,
            getNotificationQueryService.findAll(clubId = clubId, userId = userId, page = page, size = size),
        )

    @PatchMapping("/clubs/{clubId}/notifications/{notificationId}/read")
    @Operation(summary = "알림 읽음 처리")
    fun markRead(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable notificationId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void?> {
        manageNotificationUseCase.markRead(clubId = clubId, userId = userId, notificationId = notificationId)
        return CommonResponse.success(NotificationResponseCode.NOTIFICATION_READ_SUCCESS)
    }

    @PatchMapping("/clubs/{clubId}/notifications/read-all")
    @Operation(summary = "모든 알림 읽음 처리")
    fun markAllRead(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void?> {
        manageNotificationUseCase.markAllRead(clubId = clubId, userId = userId)
        return CommonResponse.success(NotificationResponseCode.NOTIFICATION_READ_ALL_SUCCESS)
    }
}
