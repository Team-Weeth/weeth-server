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

    @ExplainError("프로필 초기 설정 시 필수 필드가 누락되었을 때 발생합니다.")
    PROFILE_REQUIRED_FIELDS_MISSING(20912, HttpStatus.BAD_REQUEST, "프로필 초기 설정 시 모든 필수 항목을 입력해야 합니다."),

    @ExplainError("사용자가 LEAD인 활성 동아리를 보유한 상태로 위드 탈퇴를 시도할 때 발생합니다.")
    USER_HAS_LEAD_CLUB(20913, HttpStatus.CONFLICT, "LEAD인 동아리가 있어 탈퇴할 수 없습니다."),

    @ExplainError("프로필 ID로 조회했으나 프로필이 없거나 로그인 사용자의 소유가 아닐 때 발생합니다.")
    USER_PROFILE_NOT_FOUND(20914, HttpStatus.NOT_FOUND, "프로필을 찾을 수 없습니다."),

    @ExplainError("프로필을 설정하려는 동아리가 로그인 사용자의 ACTIVE 멤버십이 아닐 때 발생합니다.")
    USER_PROFILE_ASSIGNMENT_NOT_ALLOWED(20915, HttpStatus.FORBIDDEN, "해당 동아리에 프로필을 설정할 수 없습니다."),

    @ExplainError("동아리별 프로필 설정 요청에서 같은 동아리가 중복될 때 발생합니다.")
    USER_PROFILE_DUPLICATE_CLUB_ASSIGNMENT(20916, HttpStatus.BAD_REQUEST, "같은 동아리의 프로필 설정이 중복되었습니다."),

    @ExplainError("동아리별 프로필 설정 요청의 clubId가 Base62 TSID 형식이 아닐 때 발생합니다.")
    USER_PROFILE_INVALID_CLUB_ID(20917, HttpStatus.BAD_REQUEST, "동아리 ID 형식이 올바르지 않습니다."),
}
