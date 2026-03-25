package com.weeth.domain.board.application.exception

import com.weeth.global.common.exception.ErrorCodeInterface
import com.weeth.global.common.exception.ExplainError
import org.springframework.http.HttpStatus

enum class BoardErrorCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ErrorCodeInterface {
    @ExplainError("검색 결과가 없을 때 발생합니다.")
    NO_SEARCH_RESULT(20400, HttpStatus.NOT_FOUND, "검색 결과가 없습니다."),

    @ExplainError("유효하지 않은 페이지 번호를 요청할 때 발생합니다.")
    PAGE_NOT_FOUND(20401, HttpStatus.BAD_REQUEST, "유효하지 않은 페이지입니다."),

    @ExplainError("ADMIN 전용 게시판에 일반 사용자가 글을 작성할 때 발생합니다.")
    CATEGORY_ACCESS_DENIED(20402, HttpStatus.FORBIDDEN, "해당 카테고리에 대한 권한이 없습니다."),

    @ExplainError("게시판 ID로 조회했으나 해당 게시판이 존재하지 않거나 동아리에 속하지 않는 경우에 발생합니다.")
    BOARD_NOT_FOUND(20403, HttpStatus.NOT_FOUND, "존재하지 않는 게시판입니다."),

    @ExplainError("게시글 ID로 조회했으나 해당 게시글이 존재하지 않을 때 발생합니다.")
    POST_NOT_FOUND(20404, HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."),

    @ExplainError("게시글 작성자가 아닌 사용자가 수정/삭제를 시도할 때 발생합니다.")
    POST_NOT_OWNED(20405, HttpStatus.FORBIDDEN, "게시글 작성자만 수정/삭제할 수 있습니다."),

    @ExplainError("공지 게시판이 아닌 게시판에 읽음 처리를 시도할 때 발생합니다.")
    BOARD_TYPE_MISMATCH(20406, HttpStatus.BAD_REQUEST, "공지 게시판이 아닙니다."),

    @ExplainError("경로의 clubId와 게시판의 소속 클럽이 일치하지 않을 때 발생합니다.")
    BOARD_NOT_IN_CLUB(20407, HttpStatus.FORBIDDEN, "해당 클럽에 속한 게시판이 아닙니다."),

    @ExplainError("순서 변경 요청에 중복된 게시판 ID가 포함되어 있을 때 발생합니다.")
    DUPLICATE_BOARD_ID(20408, HttpStatus.BAD_REQUEST, "중복된 게시판 ID가 포함되어 있습니다."),

    @ExplainError("동일한 클럽 내에 같은 이름의 게시판이 이미 존재할 때 발생합니다.")
    DUPLICATE_BOARD_NAME(20409, HttpStatus.CONFLICT, "이미 존재하는 게시판 이름입니다."),

    @ExplainError("공지사항 등 고정 게시판을 순서 변경 요청에 포함할 때 발생합니다.")
    FIXED_BOARD_NOT_REORDERABLE(20410, HttpStatus.BAD_REQUEST, "고정 게시판은 순서를 변경할 수 없습니다."),
}
