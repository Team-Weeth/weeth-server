package com.weeth.domain.user.application.exception;

import com.weeth.global.common.exception.BusinessLogicException;

public class UserMismatchException extends BusinessLogicException {
    public UserMismatchException() {
        super(UserErrorCode.USER_MISMATCH);
    }
}