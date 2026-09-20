package com.weeth.domain.notification.application.exception

import com.weeth.global.common.exception.ErrorCodeInterface
import com.weeth.global.common.exception.ExplainError
import org.springframework.http.HttpStatus

enum class NotificationErrorCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ErrorCodeInterface {
    @ExplainError("요청한 알림 ID에 해당하는 알림이 없거나 현재 사용자 소유 알림이 아닐 때 발생합니다.")
    NOTIFICATION_NOT_FOUND(21400, HttpStatus.NOT_FOUND, "존재하지 않는 알림입니다."),
}
