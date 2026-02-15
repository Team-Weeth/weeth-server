package com.weeth.domain.user.application.exception;

import com.weeth.global.common.exception.BaseException;

public class StatusNotFoundException extends BaseException {
    public StatusNotFoundException() {
        super(UserErrorCode.STATUS_NOT_FOUND);
    }
}
