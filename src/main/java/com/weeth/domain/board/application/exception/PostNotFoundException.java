package com.weeth.domain.board.application.exception;

import com.weeth.global.common.exception.BaseException;

public class PostNotFoundException extends BaseException {
    public PostNotFoundException() {
        super(PostErrorCode.POST_NOT_FOUND);
    }
}
