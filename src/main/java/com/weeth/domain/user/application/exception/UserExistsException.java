package com.weeth.domain.user.application.exception;

import com.weeth.global.common.exception.BusinessLogicException;

public class UserExistsException extends BusinessLogicException {
    public UserExistsException() {
        super(UserErrorCode.USER_EXISTS);
    }
}