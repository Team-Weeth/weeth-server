package com.weeth.domain.user.application.exception;

import com.weeth.global.common.exception.BusinessLogicException;

public class DuplicateCardinalException extends BusinessLogicException {
    public DuplicateCardinalException() {
        super(UserErrorCode.DUPLICATE_CARDINAL);
    }
}
