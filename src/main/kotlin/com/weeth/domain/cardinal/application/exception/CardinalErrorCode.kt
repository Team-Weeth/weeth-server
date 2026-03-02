package com.weeth.domain.cardinal.application.exception

import com.weeth.global.common.exception.ErrorCodeInterface
import com.weeth.global.common.exception.ExplainError
import org.springframework.http.HttpStatus

enum class CardinalErrorCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ErrorCodeInterface {
    @ExplainError("존재하지 않는 기수 ID 또는 번호로 조회했을 때 발생합니다.")
    CARDINAL_NOT_FOUND(2850, HttpStatus.NOT_FOUND, "기수를 찾을 수 없습니다."),

    @ExplainError("이미 존재하는 기수를 생성하려고 할 때 발생합니다.")
    DUPLICATE_CARDINAL(2851, HttpStatus.BAD_REQUEST, "이미 존재하는 기수입니다."),
}
