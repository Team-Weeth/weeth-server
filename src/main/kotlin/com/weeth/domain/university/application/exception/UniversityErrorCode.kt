package com.weeth.domain.university.application.exception

import com.weeth.global.common.exception.ErrorCodeInterface
import com.weeth.global.common.exception.ExplainError
import org.springframework.http.HttpStatus

enum class UniversityErrorCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ErrorCodeInterface {
    @ExplainError("커리어넷 Open API 호출에 실패했을 때 발생합니다.")
    CAREER_NET_API_ERROR(31300, HttpStatus.INTERNAL_SERVER_ERROR, "학교/학과 정보를 불러오는데 실패했습니다."),
}
