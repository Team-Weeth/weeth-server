package com.weeth.domain.user.application.exception

import com.weeth.global.common.exception.ErrorCodeInterface
import com.weeth.global.common.exception.ExplainError
import org.springframework.http.HttpStatus

enum class UserErrorCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ErrorCodeInterface {
    @ExplainError("사용자 ID로 조회했으나 해당 사용자가 존재하지 않을 때 발생합니다.")
    USER_NOT_FOUND(20900, HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다."),

    @ExplainError("가입 승인 대기 중인 사용자가 접근을 시도할 때 발생합니다.")
    USER_INACTIVE(20901, HttpStatus.FORBIDDEN, "가입 승인이 허가되지 않은 계정입니다."),

    @ExplainError("이미 가입된 이메일로 회원가입을 시도할 때 발생합니다.")
    USER_EXISTS(20902, HttpStatus.BAD_REQUEST, "이미 가입된 사용자입니다."),

    @ExplainError("요청한 사용자 정보와 실제 사용자 정보가 일치하지 않을 때 발생합니다.")
    USER_MISMATCH(20903, HttpStatus.FORBIDDEN, "사용자 정보가 일치하지 않습니다."),

    @ExplainError("다른 사용자의 리소스에 접근하려고 할 때 발생합니다.")
    USER_NOT_MATCH(20904, HttpStatus.FORBIDDEN, "해당 사용자가 아닙니다."),

    @ExplainError("로그인 시 비밀번호가 일치하지 않을 때 발생합니다.")
    PASSWORD_MISMATCH(20905, HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),

    @ExplainError("입력한 이메일로 등록된 사용자가 없을 때 발생합니다.")
    EMAIL_NOT_FOUND(20906, HttpStatus.NOT_FOUND, "이메일을 찾을 수 없습니다."),

    @ExplainError("이미 등록된 학번으로 회원가입을 시도할 때 발생합니다.")
    STUDENT_ID_EXISTS(20907, HttpStatus.BAD_REQUEST, "이미 존재하는 학번입니다."),

    @ExplainError("이미 등록된 전화번호로 회원가입을 시도할 때 발생합니다.")
    TEL_EXISTS(20908, HttpStatus.BAD_REQUEST, "이미 존재하는 전화번호입니다."),

    @ExplainError("잘못된 권한 값이 입력되었을 때 발생합니다.")
    ROLE_NOT_FOUND(20909, HttpStatus.BAD_REQUEST, "권한을 찾을 수 없습니다."),

    @ExplainError("잘못된 상태 값이 입력되었을 때 발생합니다.")
    STATUS_NOT_FOUND(20910, HttpStatus.BAD_REQUEST, "상태를 찾을 수 없습니다."),

    @ExplainError("사용자 순서 지정 시 잘못된 값이 입력되었을 때 발생합니다.")
    INVALID_USER_ORDER(20911, HttpStatus.BAD_REQUEST, "잘못된 사용자 순서입니다."),

    @ExplainError("Notion API 호출에 실패했을 때 발생합니다.")
    NOTION_API_ERROR(20912, HttpStatus.INTERNAL_SERVER_ERROR, "Notion API 호출에 실패했습니다."),

    @ExplainError("Slack API 호출에 실패했을 때 발생합니다.")
    SLACK_API_ERROR(20913, HttpStatus.INTERNAL_SERVER_ERROR, "Slack 알림 전송에 실패했습니다."),
}
