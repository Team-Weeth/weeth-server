package com.weeth.domain.user.application.exception;

import com.weeth.global.common.exception.BaseException;

public class UserCardinalNotFoundException extends BaseException {
    public UserCardinalNotFoundException() {
        super(UserErrorCode.USER_CARDINAL_NOT_FOUND);
    }
}
