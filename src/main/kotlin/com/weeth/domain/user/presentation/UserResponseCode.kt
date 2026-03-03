package com.weeth.domain.user.presentation

import com.weeth.global.common.response.ResponseCodeInterface
import org.springframework.http.HttpStatus

enum class UserResponseCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ResponseCodeInterface {
    USER_FIND_ALL_SUCCESS(10900, HttpStatus.OK, "모든 회원 정보를 성공적으로 조회했습니다."),
    USER_DETAILS_SUCCESS(10901, HttpStatus.OK, "특정 회원의 상세 정보를 성공적으로 조회했습니다."),
    USER_ACCEPT_SUCCESS(10902, HttpStatus.OK, "회원 가입 승인이 성공적으로 처리되었습니다."),
    USER_BAN_SUCCESS(10903, HttpStatus.OK, "회원이 성공적으로 차단되었습니다."),
    USER_ROLE_UPDATE_SUCCESS(10904, HttpStatus.OK, "회원의 역할이 성공적으로 수정되었습니다."),
    USER_APPLY_OB_SUCCESS(10905, HttpStatus.OK, "OB 신청이 성공적으로 처리되었습니다."),
    USER_EMAIL_CHECK_SUCCESS(10906, HttpStatus.OK, "이메일 중복 검사가 성공적으로 처리되었습니다."),
    USER_FIND_BY_ID_SUCCESS(10907, HttpStatus.OK, "회원 정보가 성공적으로 조회되었습니다."),
    USER_UPDATE_SUCCESS(10908, HttpStatus.OK, "회원 정보가 성공적으로 수정되었습니다."),
    USER_LEAVE_SUCCESS(10909, HttpStatus.OK, "회원 탈퇴가 성공적으로 처리되었습니다."),
    JWT_REFRESH_SUCCESS(10910, HttpStatus.OK, "토큰 재발급에 성공했습니다."),
    SOCIAL_LOGIN_SUCCESS(10911, HttpStatus.OK, "소셜 로그인이 성공적으로 처리되었습니다."),
}
