package com.weeth.domain.user.application.exception;

import com.weeth.global.common.exception.BaseException;

public class TelExistsException extends BaseException {
    public TelExistsException() {
        super(UserErrorCode.TEL_EXISTS);
    }
}
