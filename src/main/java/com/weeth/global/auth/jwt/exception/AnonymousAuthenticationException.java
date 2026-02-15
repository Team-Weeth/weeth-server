package com.weeth.global.auth.jwt.exception;

import com.weeth.global.common.exception.BaseException;

public class AnonymousAuthenticationException extends BaseException {
    public AnonymousAuthenticationException() {
        super(JwtErrorCode.ANONYMOUS_AUTHENTICATION);
    }
}
