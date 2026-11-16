package com.weeth.domain.releasenote.application.exception

import com.weeth.global.common.exception.ErrorCodeInterface
import com.weeth.global.common.exception.ExplainError
import org.springframework.http.HttpStatus

enum class ReleaseNoteErrorCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ErrorCodeInterface {
    @ExplainError("존재하지 않거나, 공개 API로 DRAFT(미발행) 노트를 조회할 때 발생합니다.")
    RELEASE_NOTE_NOT_FOUND(21600, HttpStatus.NOT_FOUND, "릴리즈 노트를 찾을 수 없습니다."),

    @ExplainError("백오피스 API를 DEVELOPER 권한이 없는 사용자가 호출할 때 발생합니다.")
    RELEASE_NOTE_FORBIDDEN(21601, HttpStatus.FORBIDDEN, "릴리즈 노트 관리 권한이 없습니다."),
}
