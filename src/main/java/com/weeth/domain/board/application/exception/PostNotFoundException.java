package com.weeth.domain.board.application.exception;

import com.weeth.global.common.exception.BusinessLogicException;

public class PostNotFoundException extends BusinessLogicException {
    public PostNotFoundException() {
        super(PostErrorCode.POST_NOT_FOUND);
    }
}
