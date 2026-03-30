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

    @ExplainError("요청한 멤버가 해당 동아리에 속하지 않을 때 발생합니다.")
    CLUB_MEMBER_NOT_IN_CLUB(21108, HttpStatus.BAD_REQUEST, "해당 동아리에 속한 멤버가 아닙니다."),

    @ExplainError("이미 활동 기수가 설정된 멤버가 다시 설정을 시도할 때 발생합니다.")
    CARDINAL_ALREADY_SET(21109, HttpStatus.CONFLICT, "이미 활동 기수가 설정되어 있습니다."),

    @ExplainError("일반 멤버(USER)로 가입 가능한 동아리 수(최대 1개)를 초과했을 때 발생합니다.")
    CLUB_JOIN_LIMIT_EXCEEDED(21110, HttpStatus.CONFLICT, "가입 가능한 동아리 수를 초과했습니다."),

    @ExplainError("동아리장(LEAD)으로 생성 가능한 동아리 수(최대 1개)를 초과했을 때 발생합니다.")
    CLUB_CREATE_LIMIT_EXCEEDED(21111, HttpStatus.CONFLICT, "생성 가능한 동아리 수를 초과했습니다."),

    @ExplainError("주 연락처를 이메일로 설정했으나 이메일이 입력되지 않았을 때 발생합니다.")
    EMAIL_REQUIRED_FOR_PRIMARY_CONTACT(21112, HttpStatus.BAD_REQUEST, "주 연락처를 이메일로 설정하려면 이메일을 입력해야 합니다."),

    @ExplainError("LEAD가 아닌 멤버가 LEAD 이양을 시도할 때 발생합니다.")
    NOT_LEAD(21113, HttpStatus.FORBIDDEN, "LEAD만 권한을 이양할 수 있습니다."),

    @ExplainError("LEAD를 이양이 아닌 직접 역할 변경으로 설정하려 할 때 발생합니다.")
    LEAD_TRANSFER_ONLY(21114, HttpStatus.BAD_REQUEST, "LEAD는 이양을 통해서만 변경할 수 있습니다."),

    @ExplainError("자기 자신에게 LEAD 권한을 이양하려 할 때 발생합니다.")
    LEAD_SELF_TRANSFER(21115, HttpStatus.BAD_REQUEST, "자기 자신에게 LEAD를 이양할 수 없습니다."),

    @ExplainError("관리자가 자기 자신을 추방하려 할 때 발생합니다.")
    SELF_BAN_NOT_ALLOWED(21116, HttpStatus.BAD_REQUEST, "자기 자신은 추방할 수 없습니다."),

    @ExplainError("관리자가 자기 자신의 권한을 변경하려 할 때 발생합니다.")
    SELF_ROLE_CHANGE_NOT_ALLOWED(21117, HttpStatus.BAD_REQUEST, "자기 자신의 권한은 변경할 수 없습니다."),

    @ExplainError("삭제하려는 기수에 출석/결석 기록이 존재할 때 발생합니다. force=true로 재요청하면 출석 기록을 포함해 삭제됩니다.")
    CARDINAL_REMOVAL_HAS_ATTENDANCE(
        21118,
        HttpStatus.UNPROCESSABLE_ENTITY,
        "출석 기록이 있는 기수가 포함되어 있습니다. 삭제하려면 force=true로 재요청하세요.",
    ),
}
