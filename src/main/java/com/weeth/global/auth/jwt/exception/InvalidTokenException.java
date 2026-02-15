package com.weeth.global.auth.jwt.exception;

import com.weeth.global.common.exception.BusinessLogicException;

public class InvalidTokenException extends BusinessLogicException {
    public InvalidTokenException() {
        super(JwtErrorCode.INVALID_TOKEN);
    }
}
