package com.weeth.domain.schedule.application.exception

import com.weeth.global.common.exception.ErrorCodeInterface
import com.weeth.global.common.exception.ExplainError
import org.springframework.http.HttpStatus

enum class EventErrorCode(
    private val code: Int,
    private val status: HttpStatus,
    private val message: String,
) : ErrorCodeInterface {
    @ExplainError("요청한 일정 ID에 해당하는 일정이 존재하지 않을 때 발생합니다.")
    EVENT_NOT_FOUND(2700, HttpStatus.NOT_FOUND, "존재하지 않는 일정입니다."),
    ;

    override fun getCode(): Int = code

    override fun getStatus(): HttpStatus = status

    override fun getMessage(): String = message
}
