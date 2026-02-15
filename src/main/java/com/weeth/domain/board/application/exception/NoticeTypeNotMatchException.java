package com.weeth.domain.board.application.exception;

import com.weeth.global.common.exception.BusinessLogicException;

public class NoticeTypeNotMatchException extends BusinessLogicException {
    public NoticeTypeNotMatchException() {
        super(NoticeErrorCode.NOTICE_TYPE_NOT_MATCH);
    }
}
