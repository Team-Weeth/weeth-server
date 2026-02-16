package com.weeth.domain.user.application.exception;

import com.weeth.global.common.exception.BaseException;

public class PasswordMismatchException extends BaseException {
    public PasswordMismatchException() {
        super(UserErrorCode.PASSWORD_MISMATCH);
    }
}
