package com.weeth.domain.user.application.exception;

import com.weeth.global.common.exception.BusinessLogicException;

public class InvalidUserOrderException extends BusinessLogicException {
    public InvalidUserOrderException() {
        super(UserErrorCode.INVALID_USER_ORDER);
    }
}
