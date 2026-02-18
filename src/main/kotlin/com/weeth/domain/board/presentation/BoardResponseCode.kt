package com.weeth.domain.board.presentation

import com.weeth.global.common.response.ResponseCodeInterface
import org.springframework.http.HttpStatus

enum class BoardResponseCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ResponseCodeInterface {
    POST_CREATED_SUCCESS(1301, HttpStatus.OK, "게시글이 성공적으로 생성되었습니다."),
    POST_UPDATED_SUCCESS(1302, HttpStatus.OK, "게시글이 성공적으로 수정되었습니다."),
    POST_DELETED_SUCCESS(1303, HttpStatus.OK, "게시글이 성공적으로 삭제되었습니다."),
    POST_FIND_ALL_SUCCESS(1304, HttpStatus.OK, "게시글 목록이 성공적으로 조회되었습니다."),
    POST_FIND_BY_ID_SUCCESS(1305, HttpStatus.OK, "게시글이 성공적으로 조회되었습니다."),
    POST_SEARCH_SUCCESS(1306, HttpStatus.OK, "게시글 검색 결과가 성공적으로 조회되었습니다."),
}
