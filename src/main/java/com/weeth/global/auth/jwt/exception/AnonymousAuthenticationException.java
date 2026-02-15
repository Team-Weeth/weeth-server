package com.weeth.global.auth.jwt.exception;

import com.weeth.global.common.exception.BusinessLogicException;

public class AnonymousAuthenticationException extends BusinessLogicException {
    public AnonymousAuthenticationException() {
        super(JwtErrorCode.ANONYMOUS_AUTHENTICATION);
    }
}
