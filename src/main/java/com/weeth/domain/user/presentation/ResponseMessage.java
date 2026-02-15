package com.weeth.domain.user.presentation;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseMessage {
    // UserAdminController 관련
    USER_FIND_ALL_SUCCESS("모든 회원 정보를 성공적으로 조회했습니다."),
    USER_DETAILS_SUCCESS("특정 회원의 상세 정보를 성공적으로 조회했습니다."),
    USER_ACCEPT_SUCCESS("회원 가입 승인이 성공적으로 처리되었습니다."),
    USER_BAN_SUCCESS("회원이 성공적으로 차단되었습니다."),
    USER_ROLE_UPDATE_SUCCESS("회원의 역할이 성공적으로 수정되었습니다."),
    USER_APPLY_OB_SUCCESS("OB 신청이 성공적으로 처리되었습니다."),
    USER_PASSWORD_RESET_SUCCESS("비밀번호가 성공적으로 초기화되었습니다."),
    // UserController 관련
    USER_APPLY_SUCCESS("회원 가입 신청이 성공적으로 처리되었습니다."),
    USER_EMAIL_CHECK_SUCCESS("이메일 중복 검사가 성공적으로 처리되었습니다."),
    USER_FIND_BY_ID_SUCCESS("회원 정보가 성공적으로 조회되었습니다."),
    USER_UPDATE_SUCCESS("회원 정보가 성공적으로 수정되었습니다."),
    USER_LEAVE_SUCCESS("회원 탈퇴가 성공적으로 처리되었습니다."),
    SOCIAL_LOGIN_SUCCESS("소셜 로그인에 성공했습니다."),
    SOCIAL_REGISTER_SUCCESS("소셜 회원가입에 성공했습니다."),
    SOCIAL_AUTH_SUCCESS("소셜 인증에 성공했습니다."),
    SOCIAL_INTEGRATE_SUCCESS("소셜 로그인 연동에 성공했습니다."),
    JWT_REFRESH_SUCCESS("토큰 재발급에 성공했습니다."),

    // CardinalController 관련
    CARDINAL_FIND_ALL_SUCCESS("전체 기수 조회에 성공했습니다."),
    CARDINAL_SAVE_SUCCESS("기수 저장에 성공했습니다."),
    CARDINAL_UPDATE_SUCCESS("기수 수정에 성공했습니다.");

    private final String message;
}
