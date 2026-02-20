package com.weeth.domain.attendance.application.exception

import com.weeth.global.common.exception.ErrorCodeInterface
import com.weeth.global.common.exception.ExplainError
import org.springframework.http.HttpStatus

enum class AttendanceErrorCode(
    private val code: Int,
    private val status: HttpStatus,
    private val message: String,
) : ErrorCodeInterface {
    @ExplainError("출석 정보를 찾을 수 없을 때 발생합니다.")
    ATTENDANCE_NOT_FOUND(2200, HttpStatus.NOT_FOUND, "출석 정보가 존재하지 않습니다."),

    @ExplainError("입력한 출석 코드가 생성된 코드와 일치하지 않을 때 발생합니다.")
    ATTENDANCE_CODE_MISMATCH(2201, HttpStatus.BAD_REQUEST, "출석 코드가 일치하지 않습니다."),

    @ExplainError("사용자가 출석 일정을 직접 수정하려고 시도할 때 발생합니다. (출석 로직 위반)")
    ATTENDANCE_EVENT_TYPE_NOT_MATCH(2202, HttpStatus.BAD_REQUEST, "출석일정은 직접 수정할 수 없습니다."),

    @ExplainError("세션 ID로 조회했으나 해당 세션이 존재하지 않을 때 발생합니다.")
    SESSION_NOT_FOUND(2203, HttpStatus.NOT_FOUND, "존재하지 않는 세션입니다."),
    ;

    override fun getCode(): Int = code

    override fun getStatus(): HttpStatus = status

    override fun getMessage(): String = message
}
