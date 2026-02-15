package com.weeth.global.auth.jwt.exception;

import com.weeth.global.common.exception.BaseException;

public class TokenNotFoundException extends BaseException {
    public TokenNotFoundException() {
        super(JwtErrorCode.TOKEN_NOT_FOUND);
    }
}
