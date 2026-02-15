package com.weeth.global.auth.jwt.exception;

import com.weeth.global.common.exception.BusinessLogicException;

public class RedisTokenNotFoundException extends BusinessLogicException {
    public RedisTokenNotFoundException() {
        super(JwtErrorCode.REDIS_TOKEN_NOT_FOUND);
    }
}
