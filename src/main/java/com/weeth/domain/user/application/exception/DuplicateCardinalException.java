package com.weeth.domain.user.application.exception;

import com.weeth.global.common.exception.BaseException;

public class DuplicateCardinalException extends BaseException {
    public DuplicateCardinalException() {
        super(UserErrorCode.DUPLICATE_CARDINAL);
    }
}
