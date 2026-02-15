package com.weeth.domain.board.application.exception;

import com.weeth.global.common.exception.BusinessLogicException;

public class NoticeNotFoundException extends BusinessLogicException {
    public NoticeNotFoundException() {
        super(NoticeErrorCode.NOTICE_NOT_FOUND);
    }
}
