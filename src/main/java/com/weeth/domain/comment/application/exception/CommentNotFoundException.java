package com.weeth.domain.comment.application.exception;

import com.weeth.global.common.exception.BaseException;

public class CommentNotFoundException extends BaseException {
    public CommentNotFoundException() {
        super(CommentErrorCode.COMMENT_NOT_FOUND);
    }
}
