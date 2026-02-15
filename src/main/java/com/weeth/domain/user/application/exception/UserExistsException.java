package com.weeth.domain.user.application.exception;

import com.weeth.global.common.exception.BaseException;

public class UserExistsException extends BaseException {
    public UserExistsException() {
        super(UserErrorCode.USER_EXISTS);
    }
}
