package com.weeth.domain.club.application.exception

import com.weeth.global.common.exception.ErrorCodeInterface
import com.weeth.global.common.exception.ExplainError
import org.springframework.http.HttpStatus

enum class ClubErrorCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ErrorCodeInterface {
    @ExplainError("동아리 ID로 조회했으나 존재하지 않을 때 발생합니다.")
    CLUB_NOT_FOUND(21100, HttpStatus.NOT_FOUND, "존재하지 않는 동아리입니다."),

    @ExplainError("가입 신청 시 초대 코드가 일치하지 않을 때 발생합니다.")
    INVALID_CLUB_CODE(21101, HttpStatus.BAD_REQUEST, "유효하지 않은 초대 코드입니다."),

    @ExplainError("이미 가입한 동아리에 재가입 시도할 때 발생합니다.")
    ALREADY_JOINED(21102, HttpStatus.CONFLICT, "이미 가입된 동아리입니다."),

    @ExplainError("동아리 멤버가 아닌 사용자가 동아리 리소스에 접근할 때 발생합니다.")
    CLUB_MEMBER_NOT_FOUND(21103, HttpStatus.NOT_FOUND, "동아리 멤버가 아닙니다."),

    @ExplainError("동아리 관리자 권한이 필요한 작업을 일반 멤버가 시도할 때 발생합니다.")
    NOT_CLUB_ADMIN(21104, HttpStatus.FORBIDDEN, "동아리 관리자 권한이 필요합니다."),

    @ExplainError("리더가 권한 이양 없이 동아리를 탈퇴하려 할 때 발생합니다.")
    CANNOT_LEAVE_AS_LEAD(21105, HttpStatus.BAD_REQUEST, "리더는 권한 이양 후 탈퇴할 수 있습니다."),

    @ExplainError("비활성 멤버가 동아리 리소스에 접근할 때 발생합니다.")
    MEMBER_NOT_ACTIVE(21106, HttpStatus.FORBIDDEN, "비활성 멤버입니다."),

    @ExplainError("MVP 단계에서 여러 동아리에 지원하려고 하는 경우 발생합니다. MVP는 단일 동아리 지원만 가능합니다.")
    CLUB_CANT_JOIN(21107, HttpStatus.BAD_REQUEST, "MVP에서 동아리는 1개만 지원 가능합니다."),
}
