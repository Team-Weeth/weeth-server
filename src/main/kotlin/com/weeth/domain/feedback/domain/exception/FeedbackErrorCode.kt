package com.weeth.domain.feedback.domain.exception

import com.weeth.global.common.exception.ErrorCodeInterface
import com.weeth.global.common.exception.ExplainError
import org.springframework.http.HttpStatus

enum class FeedbackErrorCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ErrorCodeInterface {
    @ExplainError(
        "피드백 내용이 공백뿐이거나(유니코드 공백 포함) 1000자를 초과할 때 발생합니다. " +
            "`@NotBlank`는 일부 유니코드 공백(예: U+3000)을 공백으로 인식하지 못해 엔티티 단계에서 최종 검증합니다.",
    )
    INVALID_FEEDBACK_CONTENT(21500, HttpStatus.BAD_REQUEST, "피드백 내용이 올바르지 않습니다."),
}
