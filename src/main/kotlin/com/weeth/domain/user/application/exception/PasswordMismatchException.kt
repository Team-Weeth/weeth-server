package com.weeth.domain.user.application.exception

import com.weeth.global.common.exception.BaseException

class PasswordMismatchException : BaseException(UserErrorCode.PASSWORD_MISMATCH)
