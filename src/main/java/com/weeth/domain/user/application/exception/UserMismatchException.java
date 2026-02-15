package com.weeth.domain.user.application.exception;

import com.weeth.global.common.exception.BaseException;

public class UserMismatchException extends BaseException {
    public UserMismatchException() {
        super(UserErrorCode.USER_MISMATCH);
    }
}
