package com.weeth.domain.board.application.exception;

import com.weeth.global.common.exception.ErrorCodeInterface;
import com.weeth.global.common.exception.ExplainError;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BoardErrorCode implements ErrorCodeInterface {

    @ExplainError("검색 조건에 맞는 게시글이 하나도 없을 때 발생합니다.")
    NO_SEARCH_RESULT(2300, HttpStatus.NOT_FOUND, "일치하는 검색 결과를 찾을 수 없습니다."),

    @ExplainError("요청한 페이지 번호가 유효 범위를 벗어났을 때 발생합니다.")
    PAGE_NOT_FOUND(2301, HttpStatus.NOT_FOUND, "존재하지 않는 페이지입니다."),

    @ExplainError("일반 유저가 어드민 전용 카테고리에 접근하려 할 때 발생합니다.")
    CATEGORY_ACCESS_DENIED(2302, HttpStatus.FORBIDDEN, "어드민 유저만 접근 가능한 카테고리입니다");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
