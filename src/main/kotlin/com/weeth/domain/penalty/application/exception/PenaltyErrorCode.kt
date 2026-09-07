package com.weeth.domain.penalty.application.exception

import com.weeth.global.common.exception.ErrorCodeInterface
import com.weeth.global.common.exception.ExplainError
import org.springframework.http.HttpStatus

enum class PenaltyErrorCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ErrorCodeInterface {
    @ExplainError("요청한 페널티 ID가 존재하지 않을 때 발생합니다.")
    PENALTY_NOT_FOUND(20700, HttpStatus.NOT_FOUND, "존재하지 않는 페널티입니다."),

    @ExplainError("시스템에 의해 자동 부여된 페널티를 수동으로 삭제하려 할 때 발생합니다.")
    AUTO_PENALTY_DELETE_NOT_ALLOWED(20701, HttpStatus.BAD_REQUEST, "자동 생성된 페널티는 삭제할 수 없습니다"),

    @ExplainError("경고 기능이 활성화되지 않은 동아리에서 경고를 부여하려 할 때 발생합니다.")
    WARNING_NOT_ENABLED(20702, HttpStatus.FORBIDDEN, "해당 동아리에서는 경고 기능을 사용할 수 없습니다."),
}
