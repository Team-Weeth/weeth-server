package com.weeth.global.auth.jwt.application.exception

import com.weeth.global.common.exception.BaseException

class TokenNotFoundException : BaseException(JwtErrorCode.TOKEN_NOT_FOUND)
