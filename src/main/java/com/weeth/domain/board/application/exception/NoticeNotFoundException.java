package com.weeth.domain.board.application.exception;

import com.weeth.global.common.exception.BaseException;

public class NoticeNotFoundException extends BaseException {
    public NoticeNotFoundException() {
        super(NoticeErrorCode.NOTICE_NOT_FOUND);
    }
}
