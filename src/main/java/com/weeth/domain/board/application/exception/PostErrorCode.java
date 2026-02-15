package com.weeth.domain.board.application.exception;

import com.weeth.global.common.exception.ErrorCodeInterface;
import com.weeth.global.common.exception.ExplainError;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PostErrorCode implements ErrorCodeInterface {

    @ExplainError("요청한 게시글 ID에 해당하는 게시글이 없을 때 발생합니다.")
    POST_NOT_FOUND(404, HttpStatus.NOT_FOUND, "존재하지 않는 게시물입니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
