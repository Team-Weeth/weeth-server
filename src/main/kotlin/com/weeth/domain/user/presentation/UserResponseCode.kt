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
    USER_LEFT_SUCCESS(10906, HttpStatus.OK, "위드 탈퇴가 완료되었습니다."),
    USER_PROFILE_CREATED_SUCCESS(10907, HttpStatus.CREATED, "프로필이 성공적으로 생성되었습니다."),
    USER_PROFILE_FIND_ALL_SUCCESS(10908, HttpStatus.OK, "프로필 목록을 성공적으로 조회했습니다."),
    USER_PROFILE_FIND_SUCCESS(10909, HttpStatus.OK, "프로필을 성공적으로 조회했습니다."),
    USER_PROFILE_UPDATED_SUCCESS(10910, HttpStatus.OK, "프로필이 성공적으로 수정되었습니다."),
    USER_PROFILE_ASSIGNMENT_UPDATED_SUCCESS(10911, HttpStatus.OK, "동아리별 사용 프로필이 성공적으로 변경되었습니다."),
}
