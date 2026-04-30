package com.weeth.global.auth.jwt.application.exception

import com.weeth.global.common.exception.BaseException

class InvalidTokenException : BaseException(JwtErrorCode.INVALID_TOKEN)
