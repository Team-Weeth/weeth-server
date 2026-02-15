package com.weeth.domain.comment.application.exception;

import com.weeth.global.common.exception.BusinessLogicException;

public class CommentNotFoundException extends BusinessLogicException {
    public CommentNotFoundException() {
        super(CommentErrorCode.COMMENT_NOT_FOUND);
    }
}
