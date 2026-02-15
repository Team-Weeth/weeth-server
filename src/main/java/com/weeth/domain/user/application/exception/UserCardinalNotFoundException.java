package com.weeth.domain.user.application.exception;

import com.weeth.global.common.exception.BusinessLogicException;

public class UserCardinalNotFoundException extends BusinessLogicException {
    public UserCardinalNotFoundException() {
        super(UserErrorCode.USER_CARDINAL_NOT_FOUND);
    }
}
