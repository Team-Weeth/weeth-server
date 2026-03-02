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
    SESSION_NOT_FOUND(2205, HttpStatus.NOT_FOUND, "존재하지 않는 정기모임입니다."),
}
