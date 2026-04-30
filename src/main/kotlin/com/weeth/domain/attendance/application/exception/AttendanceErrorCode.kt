package com.weeth.domain.attendance.application.exception

import com.weeth.global.common.exception.ErrorCodeInterface
import com.weeth.global.common.exception.ExplainError
import org.springframework.http.HttpStatus

enum class AttendanceErrorCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ErrorCodeInterface {
    @ExplainError("출석 정보를 찾을 수 없을 때 발생합니다.")
    ATTENDANCE_NOT_FOUND(20200, HttpStatus.NOT_FOUND, "출석 정보가 존재하지 않습니다."),

    @ExplainError("입력한 출석 코드가 생성된 코드와 일치하지 않을 때 발생합니다.")
    ATTENDANCE_CODE_MISMATCH(20201, HttpStatus.BAD_REQUEST, "출석 코드가 일치하지 않습니다."),

    @ExplainError("사용자가 출석 일정을 직접 수정하려고 시도할 때 발생합니다. (출석 로직 위반)")
    ATTENDANCE_EVENT_TYPE_NOT_MATCH(20202, HttpStatus.BAD_REQUEST, "출석일정은 직접 수정할 수 없습니다."),

    @ExplainError("QR 코드가 만료되었거나 어드민이 아직 QR을 생성하지 않았을 때 발생합니다.")
    QR_TOKEN_EXPIRED(20203, HttpStatus.BAD_REQUEST, "QR 코드가 만료되었거나 존재하지 않습니다."),

    @ExplainError("해당 세션에 이미 출석 처리된 사용자가 다시 출석을 시도할 때 발생합니다.")
    ALREADY_ATTENDED(20204, HttpStatus.CONFLICT, "이미 출석 처리된 세션입니다."),

    @ExplainError("출석이 자동 마감 처리된 후 체크인을 시도할 때 발생합니다.")
    ATTENDANCE_ALREADY_CLOSED(20205, HttpStatus.CONFLICT, "이미 마감된 출석입니다."),
}
