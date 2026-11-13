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

    @ExplainError("동일 카테고리를 한 변경 요청에 여러 번 지정했을 때 발생합니다.")
    NOTIFICATION_DUPLICATE_CATEGORY(21401, HttpStatus.BAD_REQUEST, "중복된 알림 카테고리입니다."),

    @ExplainError("변경 후 마스터 설정이 OFF인 상태에서 카테고리 선택값을 변경하려 할 때 발생합니다.")
    NOTIFICATION_MASTER_DISABLED(21402, HttpStatus.BAD_REQUEST, "전체 알림을 켠 후 카테고리 설정을 변경해주세요."),

    @ExplainError("동아리에서 사용하지 않거나 부원에게 공개하지 않은 기능의 알림 설정을 변경할 때 발생합니다.")
    NOTIFICATION_CATEGORY_UNAVAILABLE(21403, HttpStatus.BAD_REQUEST, "사용할 수 없는 알림 카테고리입니다."),

    @ExplainError("동일 회원의 알림 설정 동시 갱신 중 잠금 대기 시간을 초과했을 때 발생합니다.")
    NOTIFICATION_PREFERENCE_LOCK_TIMEOUT(21404, HttpStatus.CONFLICT, "알림 설정 변경 중입니다. 잠시 후 다시 시도해주세요."),

    @ExplainError("변경 항목의 필수 값이 없거나, 지원하지 않는 카테고리 또는 잘못된 JSON 형식일 때 발생합니다.")
    NOTIFICATION_PREFERENCE_INVALID(21405, HttpStatus.BAD_REQUEST, "알림 설정 요청 형식이 올바르지 않습니다."),
}
