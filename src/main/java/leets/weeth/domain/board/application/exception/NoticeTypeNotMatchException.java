package leets.weeth.domain.board.application.exception;

import leets.weeth.global.common.exception.BusinessLogicException;

public class NoticeTypeNotMatchException extends BusinessLogicException {
    public NoticeTypeNotMatchException() {
        super(NoticeErrorCode.NOTICE_TYPE_NOT_MATCH);
    }
}
