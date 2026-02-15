package com.weeth.global.auth.jwt.exception;

import com.weeth.global.common.exception.BusinessLogicException;

public class TokenNotFoundException extends BusinessLogicException {
    public TokenNotFoundException() {
        super(JwtErrorCode.TOKEN_NOT_FOUND);
    }
}
