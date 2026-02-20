package com.weeth.global.auth.jwt.application.exception

import com.weeth.global.common.exception.BaseException

class RedisTokenNotFoundException : BaseException(JwtErrorCode.REDIS_TOKEN_NOT_FOUND)
