package com.weeth.domain.board.application.exception;

import com.weeth.global.common.exception.BaseException;

public class NoticeTypeNotMatchException extends BaseException {
    public NoticeTypeNotMatchException() {
        super(NoticeErrorCode.NOTICE_TYPE_NOT_MATCH);
    }
}
