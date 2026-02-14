package leets.weeth.domain.comment.application.exception;

import leets.weeth.global.common.exception.ErrorCodeInterface;
import leets.weeth.global.common.exception.ExplainError;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CommentErrorCode implements ErrorCodeInterface {

    @ExplainError("요청한 댓글 ID에 해당하는 댓글이 존재하지 않을 때 발생합니다.")
    COMMENT_NOT_FOUND(404, HttpStatus.NOT_FOUND, "존재하지 않는 댓글입니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
