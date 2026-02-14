package leets.weeth.domain.board.application.exception;

import leets.weeth.global.common.exception.ErrorCodeInterface;
import leets.weeth.global.common.exception.ExplainError;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NoticeErrorCode implements ErrorCodeInterface {

    @ExplainError("요청한 공지사항 ID에 해당하는 공지사항이 없을 때 발생합니다.")
    NOTICE_NOT_FOUND(404, HttpStatus.NOT_FOUND, "존재하지 않는 공지사항입니다."),

    @ExplainError("일반 게시판에서 공지사항을 수정하려 하거나, 그 반대의 경우 발생합니다.")
    NOTICE_TYPE_NOT_MATCH(400, HttpStatus.BAD_REQUEST, "공지사항은 공지사항 게시판에서 수정하세요.");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
