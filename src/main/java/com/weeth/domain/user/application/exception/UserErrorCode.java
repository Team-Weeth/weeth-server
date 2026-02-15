package com.weeth.domain.user.application.exception;

import com.weeth.global.common.exception.ErrorCodeInterface;
import com.weeth.global.common.exception.ExplainError;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements ErrorCodeInterface {
    // User 관련 에러
    @ExplainError("사용자 ID로 조회했으나 해당 사용자가 존재하지 않을 때 발생합니다.")
    USER_NOT_FOUND(2800, HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다."),

    @ExplainError("가입 승인 대기 중인 사용자가 접근을 시도할 때 발생합니다.")
    USER_INACTIVE(2801, HttpStatus.FORBIDDEN, "가입 승인이 허가되지 않은 계정입니다."),

    @ExplainError("이미 가입된 이메일로 회원가입을 시도할 때 발생합니다.")
    USER_EXISTS(2802, HttpStatus.BAD_REQUEST, "이미 가입된 사용자입니다."),

    @ExplainError("요청한 사용자 정보와 실제 사용자 정보가 일치하지 않을 때 발생합니다.")
    USER_MISMATCH(2803, HttpStatus.FORBIDDEN, "사용자 정보가 일치하지 않습니다."),

    @ExplainError("다른 사용자의 리소스에 접근하려고 할 때 발생합니다.")
    USER_NOT_MATCH(2804, HttpStatus.FORBIDDEN, "해당 사용자가 아닙니다."),

    // 인증 관련 에러
    @ExplainError("로그인 시 비밀번호가 일치하지 않을 때 발생합니다.")
    PASSWORD_MISMATCH(2805, HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),

    @ExplainError("입력한 이메일로 등록된 사용자가 없을 때 발생합니다.")
    EMAIL_NOT_FOUND(2806, HttpStatus.NOT_FOUND, "이메일을 찾을 수 없습니다."),

    // 검증 에러
    @ExplainError("이미 등록된 학번으로 회원가입을 시도할 때 발생합니다.")
    STUDENT_ID_EXISTS(2807, HttpStatus.BAD_REQUEST, "이미 존재하는 학번입니다."),

    @ExplainError("이미 등록된 전화번호로 회원가입을 시도할 때 발생합니다.")
    TEL_EXISTS(2808, HttpStatus.BAD_REQUEST, "이미 존재하는 전화번호입니다."),

    // Cardinal 관련 에러
    @ExplainError("존재하지 않는 기수 정보로 조회할 때 발생합니다.")
    CARDINAL_NOT_FOUND(2809, HttpStatus.NOT_FOUND, "기수를 찾을 수 없습니다."),

    @ExplainError("이미 존재하는 기수를 생성하려고 할 때 발생합니다.")
    DUPLICATE_CARDINAL(2810, HttpStatus.BAD_REQUEST, "이미 존재하는 기수입니다."),

    @ExplainError("사용자와 기수 간의 연결 정보를 찾을 수 없을 때 발생합니다.")
    USER_CARDINAL_NOT_FOUND(2811, HttpStatus.NOT_FOUND, "사용자 기수 정보를 찾을 수 없습니다."),

    // Enum 관련 에러
    @ExplainError("잘못된 학과 값이 입력되었을 때 발생합니다.")
    DEPARTMENT_NOT_FOUND(2812, HttpStatus.BAD_REQUEST, "학과를 찾을 수 없습니다."),

    @ExplainError("잘못된 권한 값이 입력되었을 때 발생합니다.")
    ROLE_NOT_FOUND(2813, HttpStatus.BAD_REQUEST, "권한을 찾을 수 없습니다."),

    @ExplainError("잘못된 상태 값이 입력되었을 때 발생합니다.")
    STATUS_NOT_FOUND(2814, HttpStatus.BAD_REQUEST, "상태를 찾을 수 없습니다."),

    @ExplainError("사용자 순서 지정 시 잘못된 값이 입력되었을 때 발생합니다.")
    INVALID_USER_ORDER(2815, HttpStatus.BAD_REQUEST, "잘못된 사용자 순서입니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
