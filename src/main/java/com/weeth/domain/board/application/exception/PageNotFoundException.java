package com.weeth.domain.board.application.exception;

import com.weeth.global.common.exception.BaseException;

public class PageNotFoundException extends BaseException {
    public PageNotFoundException() {
        super(BoardErrorCode.PAGE_NOT_FOUND);
    }
}
