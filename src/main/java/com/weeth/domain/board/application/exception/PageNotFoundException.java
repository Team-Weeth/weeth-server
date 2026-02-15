package com.weeth.domain.board.application.exception;

import com.weeth.global.common.exception.BusinessLogicException;

public class PageNotFoundException extends BusinessLogicException {
    public PageNotFoundException() {
        super(BoardErrorCode.PAGE_NOT_FOUND);
    }
}
