package com.weeth.domain.session.application.exception

import com.weeth.global.common.exception.ErrorCodeInterface
import com.weeth.global.common.exception.ExplainError
import org.springframework.http.HttpStatus

enum class SessionErrorCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ErrorCodeInterface {
    @ExplainError("요청한 정기모임 ID에 해당하는 정기모임이 존재하지 않을 때 발생합니다.")
    SESSION_NOT_FOUND(2203, HttpStatus.NOT_FOUND, "존재하지 않는 정기모임입니다."),

    @ExplainError("출석 요청 시각이 정기모임 시작 10분 전 ~ 종료 10분 후 범위를 벗어날 때 발생합니다.")
    SESSION_NOT_IN_PROGRESS(2206, HttpStatus.BAD_REQUEST, "출석 가능한 시간이 아닙니다."),
}
