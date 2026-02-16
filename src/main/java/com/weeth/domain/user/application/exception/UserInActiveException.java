package com.weeth.domain.user.application.exception;

import com.weeth.global.common.exception.BaseException;

public class UserInActiveException extends BaseException {
    public UserInActiveException() {
        super(UserErrorCode.USER_INACTIVE);
    }
}
