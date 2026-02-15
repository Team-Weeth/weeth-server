package com.weeth.domain.user.application.exception;

import com.weeth.global.common.exception.BusinessLogicException;

public class UserNotMatchException extends BusinessLogicException {
    public UserNotMatchException() {
        super(UserErrorCode.USER_NOT_MATCH);
    }
}
