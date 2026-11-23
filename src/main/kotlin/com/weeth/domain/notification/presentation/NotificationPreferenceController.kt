package com.weeth.domain.notification.presentation

import com.weeth.domain.club.application.exception.ClubErrorCode
import com.weeth.domain.notification.application.dto.request.UpdateNotificationPreferencesRequest
import com.weeth.domain.notification.application.dto.response.NotificationPreferencesResponse
import com.weeth.domain.notification.application.exception.NotificationErrorCode
import com.weeth.domain.notification.application.usecase.command.ManageNotificationPreferenceUseCase
import com.weeth.domain.notification.application.usecase.query.GetNotificationPreferenceQueryService
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.auth.jwt.application.exception.JwtErrorCode
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import com.weeth.global.common.web.TsidParam
import com.weeth.global.common.web.TsidPathVariable
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "NOTIFICATION", description = "알림 API")
@RestController
@RequestMapping("/api/v4/clubs/{clubId}/notifications/preferences")
@ApiErrorCodeExample(NotificationErrorCode::class, ClubErrorCode::class, JwtErrorCode::class)
class NotificationPreferenceController(
    private val queryService: GetNotificationPreferenceQueryService,
    private val managePreferenceUseCase: ManageNotificationPreferenceUseCase,
) {
    @GetMapping
    @Operation(
        summary = "동아리별 알림 수신 설정 조회",
        description = "마스터와 모든 카테고리를 반환합니다. available=false인 카테고리는 숨기고 저장된 enabled는 유지해주세요.",
    )
    fun find(
        @TsidParam @TsidPathVariable clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<NotificationPreferencesResponse> =
        CommonResponse.success(
            NotificationResponseCode.NOTIFICATION_PREFERENCES_FIND_SUCCESS,
            queryService.find(clubId, userId),
        )

    @PatchMapping
    @Operation(
        summary = "동아리별 알림 수신 설정 변경",
        description = "생략/null은 변경하지 않습니다. 마스터 OFF 상태의 카테고리 변경은 거부합니다. 마스터 변경은 기기 토큰에 영향을 주지 않습니다.",
    )
    fun update(
        @TsidParam @TsidPathVariable clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @Valid @RequestBody request: UpdateNotificationPreferencesRequest,
    ): CommonResponse<Void?> {
        managePreferenceUseCase.update(clubId, userId, request)
        return CommonResponse.success(NotificationResponseCode.NOTIFICATION_PREFERENCES_UPDATED_SUCCESS)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleInvalidRequest(): CommonResponse<Void?> =
        CommonResponse.error(NotificationErrorCode.NOTIFICATION_PREFERENCE_INVALID)
}
