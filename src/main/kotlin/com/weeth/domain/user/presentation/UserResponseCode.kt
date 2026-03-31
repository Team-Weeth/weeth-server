package com.weeth.domain.user.presentation

import com.weeth.global.common.response.ResponseCodeInterface
import org.springframework.http.HttpStatus

enum class UserResponseCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ResponseCodeInterface {
    USER_UPDATE_SUCCESS(10901, HttpStatus.OK, "회원 정보가 성공적으로 수정되었습니다."),
    JWT_REFRESH_SUCCESS(10902, HttpStatus.OK, "토큰 재발급에 성공했습니다."),
    SOCIAL_LOGIN_SUCCESS(10903, HttpStatus.OK, "소셜 로그인이 성공적으로 처리되었습니다."),
    USER_TERMS_AGREE_SUCCESS(10904, HttpStatus.OK, "약관 동의가 성공적으로 처리되었습니다."),
    INQUIRY_SEND_SUCCESS(10905, HttpStatus.OK, "문의가 성공적으로 접수되었습니다."),
}
