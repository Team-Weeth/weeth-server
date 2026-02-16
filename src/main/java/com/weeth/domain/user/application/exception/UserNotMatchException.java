package com.weeth.domain.user.application.exception;

import com.weeth.global.common.exception.BaseException;

public class UserNotMatchException extends BaseException {
    public UserNotMatchException() {
        super(UserErrorCode.USER_NOT_MATCH);
    }
}
