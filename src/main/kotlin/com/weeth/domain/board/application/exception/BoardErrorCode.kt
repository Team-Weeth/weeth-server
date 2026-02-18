package com.weeth.domain.board.application.exception

import com.weeth.global.common.exception.ErrorCodeInterface
import com.weeth.global.common.exception.ExplainError
import org.springframework.http.HttpStatus

enum class BoardErrorCode(
    private val code: Int,
    private val status: HttpStatus,
    private val message: String,
) : ErrorCodeInterface {
    @ExplainError("검색 결과가 없을 때 발생합니다.")
    NO_SEARCH_RESULT(2300, HttpStatus.NOT_FOUND, "검색 결과가 없습니다."),

    @ExplainError("유효하지 않은 페이지 번호를 요청할 때 발생합니다.")
    PAGE_NOT_FOUND(2301, HttpStatus.BAD_REQUEST, "유효하지 않은 페이지입니다."),

    @ExplainError("ADMIN 전용 게시판에 일반 사용자가 글을 작성할 때 발생합니다.")
    CATEGORY_ACCESS_DENIED(2302, HttpStatus.FORBIDDEN, "해당 카테고리에 대한 권한이 없습니다."),

    @ExplainError("게시판 ID로 조회했으나 해당 게시판이 존재하지 않을 때 발생합니다.")
    BOARD_NOT_FOUND(2303, HttpStatus.NOT_FOUND, "존재하지 않는 게시판입니다."),

    @ExplainError("게시글 ID로 조회했으나 해당 게시글이 존재하지 않을 때 발생합니다.")
    POST_NOT_FOUND(2304, HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."),

    @ExplainError("게시글 작성자가 아닌 사용자가 수정/삭제를 시도할 때 발생합니다.")
    POST_NOT_OWNED(2305, HttpStatus.FORBIDDEN, "게시글 작성자만 수정/삭제할 수 있습니다."),
    ;

    override fun getCode(): Int = code

    override fun getStatus(): HttpStatus = status

    override fun getMessage(): String = message
}
