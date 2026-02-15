package com.weeth.domain.user.application.exception;

import com.weeth.global.common.exception.BusinessLogicException;

public class TelExistsException extends BusinessLogicException {
    public TelExistsException() {
        super(UserErrorCode.TEL_EXISTS);
    }
}
