package com.weeth.domain.board.presentation

import com.weeth.global.common.response.ResponseCodeInterface
import org.springframework.http.HttpStatus

enum class BoardResponseCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ResponseCodeInterface {
    BOARD_CREATED_SUCCESS(10400, HttpStatus.OK, "게시판이 성공적으로 생성되었습니다."),
    POST_CREATED_SUCCESS(10401, HttpStatus.OK, "게시글이 성공적으로 생성되었습니다."),
    POST_UPDATED_SUCCESS(10402, HttpStatus.OK, "게시글이 성공적으로 수정되었습니다."),
    POST_DELETED_SUCCESS(10403, HttpStatus.OK, "게시글이 성공적으로 삭제되었습니다."),
    POST_FIND_ALL_SUCCESS(10404, HttpStatus.OK, "게시글 목록이 성공적으로 조회되었습니다."),
    POST_FIND_BY_ID_SUCCESS(10405, HttpStatus.OK, "게시글이 성공적으로 조회되었습니다."),
    POST_SEARCH_SUCCESS(10406, HttpStatus.OK, "게시글 검색 결과가 성공적으로 조회되었습니다."),
    BOARD_UPDATED_SUCCESS(10407, HttpStatus.OK, "게시판이 성공적으로 수정되었습니다."),
    BOARD_DELETED_SUCCESS(10408, HttpStatus.OK, "게시판이 성공적으로 삭제되었습니다."),
    BOARD_FIND_ALL_SUCCESS(10409, HttpStatus.OK, "게시판 목록이 성공적으로 조회되었습니다."),
    BOARD_FIND_BY_ID_SUCCESS(10410, HttpStatus.OK, "게시판이 성공적으로 조회되었습니다."),
    BOARD_NOTICE_READ_SUCCESS(10411, HttpStatus.OK, "공지를 읽음 처리했습니다."),
    POST_LIKE_TOGGLE_SUCCESS(10412, HttpStatus.OK, "게시글 좋아요가 처리되었습니다."),
}
