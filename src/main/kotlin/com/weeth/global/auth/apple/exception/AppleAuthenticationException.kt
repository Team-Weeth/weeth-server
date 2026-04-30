package com.weeth.global.auth.apple.exception

import com.weeth.global.auth.jwt.application.exception.JwtErrorCode
import com.weeth.global.common.exception.BaseException

class AppleAuthenticationException : BaseException(JwtErrorCode.APPLE_AUTHENTICATION_FAILED)
