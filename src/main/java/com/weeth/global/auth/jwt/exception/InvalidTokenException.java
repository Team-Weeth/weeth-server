package com.weeth.global.auth.jwt.exception;

import com.weeth.global.common.exception.BaseException;

public class InvalidTokenException extends BaseException {
    public InvalidTokenException() {
        super(JwtErrorCode.INVALID_TOKEN);
    }
}
