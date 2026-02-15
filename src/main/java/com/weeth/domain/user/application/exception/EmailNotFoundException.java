package com.weeth.domain.user.application.exception;

import com.weeth.global.common.exception.BaseException;

public class EmailNotFoundException extends BaseException {
    public EmailNotFoundException() {
        super(UserErrorCode.EMAIL_NOT_FOUND);
    }
}
