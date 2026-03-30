package com.weeth.domain.session.application.exception

import com.weeth.global.common.exception.ErrorCodeInterface
import com.weeth.global.common.exception.ExplainError
import org.springframework.http.HttpStatus

enum class SessionErrorCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ErrorCodeInterface {
    @ExplainError("요청한 정기모임 ID에 해당하는 정기모임이 존재하지 않을 때 발생합니다.")
    SESSION_NOT_FOUND(20300, HttpStatus.NOT_FOUND, "존재하지 않는 정기모임입니다."),

    @ExplainError("출석 요청 시각이 정기모임 시작 10분 전 ~ 종료 10분 후 범위를 벗어날 때 발생합니다.")
    SESSION_NOT_IN_PROGRESS(20301, HttpStatus.BAD_REQUEST, "출석 가능한 시간이 아닙니다."),

    @ExplainError("반복 설정 시 반복 종료일이 필수인데 제공되지 않았을 때 발생합니다.")
    RECURRENCE_END_DATE_REQUIRED(20302, HttpStatus.BAD_REQUEST, "반복 종료일은 필수입니다."),

    @ExplainError("반복 종료일이 세션 시작일 이전이거나 같을 때 발생합니다.")
    RECURRENCE_END_DATE_BEFORE_START(20303, HttpStatus.BAD_REQUEST, "반복 종료일은 시작일 이후여야 합니다."),

    @ExplainError("요청한 세션 그룹 ID에 해당하는 세션 그룹이 존재하지 않을 때 발생합니다.")
    SESSION_GROUP_NOT_FOUND(20304, HttpStatus.NOT_FOUND, "존재하지 않는 세션 그룹입니다."),

    @ExplainError("THIS_AND_FUTURE 수정 범위에 이미 진행된(CLOSED) 세션이 포함될 때 발생합니다. force=true로 재요청하면 포함하여 수정합니다.")
    CLOSED_SESSION_INCLUDED_IN_UPDATE(
        20305,
        HttpStatus.CONFLICT,
        "이미 진행된 세션이 수정 범위에 포함되어 있습니다. 계속하려면 force=true로 요청하세요.",
    ),

    @ExplainError("THIS_AND_FUTURE 삭제 범위에 이미 진행된(CLOSED) 세션이 포함될 때 발생합니다. force=true로 재요청하면 포함하여 삭제합니다.")
    CLOSED_SESSION_INCLUDED_IN_DELETE(
        20306,
        HttpStatus.CONFLICT,
        "이미 진행된 세션이 삭제 범위에 포함되어 있습니다. 계속하려면 force=true로 요청하세요.",
    ),

    @ExplainError("반복 종료일이 시작일 기준 1년을 초과할 때 발생합니다.")
    RECURRENCE_END_DATE_EXCEEDS_MAX(20307, HttpStatus.BAD_REQUEST, "반복 종료일은 시작일 기준 최대 1년까지 설정할 수 있습니다."),

    @ExplainError("종료 시간이 시작 시간보다 앞설 때 발생합니다. start만 변경할 경우 end도 함께 전달해야 합니다.")
    END_BEFORE_START(20308, HttpStatus.BAD_REQUEST, "종료 시간은 시작 시간 이후여야 합니다."),
}
