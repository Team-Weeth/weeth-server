package com.weeth.global.auth.jwt.exception;

import com.weeth.global.common.exception.BaseException;

public class RedisTokenNotFoundException extends BaseException {
    public RedisTokenNotFoundException() {
        super(JwtErrorCode.REDIS_TOKEN_NOT_FOUND);
    }
}
