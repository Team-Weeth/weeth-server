package com.weeth.domain.notification.presentation

import com.weeth.global.common.response.ResponseCodeInterface
import org.springframework.http.HttpStatus

enum class NotificationResponseCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ResponseCodeInterface {
    NOTIFICATION_TOKEN_REGISTERED_SUCCESS(11400, HttpStatus.OK, "알림 토큰이 등록되었습니다."),
    NOTIFICATION_TOKEN_REVOKED_SUCCESS(11401, HttpStatus.OK, "알림 토큰이 해제되었습니다."),
}
