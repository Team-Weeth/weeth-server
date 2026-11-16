package com.weeth.domain.feedback.presentation

import com.weeth.global.common.response.ResponseCodeInterface
import org.springframework.http.HttpStatus

enum class FeedbackResponseCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ResponseCodeInterface {
    FEEDBACK_CREATED_SUCCESS(11500, HttpStatus.OK, "피드백이 성공적으로 저장되었습니다."),
}
