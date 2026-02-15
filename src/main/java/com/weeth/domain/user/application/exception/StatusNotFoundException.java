package com.weeth.domain.user.application.exception;

import com.weeth.global.common.exception.BusinessLogicException;

public class StatusNotFoundException extends BusinessLogicException {
    public StatusNotFoundException() {
        super(UserErrorCode.STATUS_NOT_FOUND);
    }
}
