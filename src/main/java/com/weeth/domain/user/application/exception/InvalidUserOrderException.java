package com.weeth.domain.user.application.exception;

import com.weeth.global.common.exception.BaseException;

public class InvalidUserOrderException extends BaseException {
    public InvalidUserOrderException() {
        super(UserErrorCode.INVALID_USER_ORDER);
    }
}
