package leets.weeth.domain.board.application.exception;

import leets.weeth.global.common.exception.BusinessLogicException;

public class NoticeNotFoundException extends BusinessLogicException {
    public NoticeNotFoundException() {
        super(NoticeErrorCode.NOTICE_NOT_FOUND);
    }
}
