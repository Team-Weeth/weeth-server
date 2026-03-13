package com.weeth.domain.dashboard.application.exception

import com.weeth.global.common.exception.ErrorCodeInterface
import com.weeth.global.common.exception.ExplainError
import org.springframework.http.HttpStatus

enum class DashboardErrorCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ErrorCodeInterface {
    @ExplainError("사용자가 해당 동아리의 활성 멤버가 아닐 때 발생합니다.")
    NOT_CLUB_MEMBER(21200, HttpStatus.FORBIDDEN, "해당 동아리의 멤버가 아닙니다."),
}
