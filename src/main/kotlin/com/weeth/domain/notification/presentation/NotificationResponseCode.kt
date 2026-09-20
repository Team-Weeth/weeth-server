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
    NOTIFICATION_LIST_SUCCESS(11402, HttpStatus.OK, "알림 목록을 조회했습니다."),
    NOTIFICATION_UNREAD_COUNT_SUCCESS(11403, HttpStatus.OK, "읽지 않은 알림 개수를 조회했습니다."),
    NOTIFICATION_READ_SUCCESS(11404, HttpStatus.OK, "알림을 읽음 처리했습니다."),
    NOTIFICATION_READ_ALL_SUCCESS(11405, HttpStatus.OK, "모든 알림을 읽음 처리했습니다."),
}
