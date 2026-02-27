package com.weeth.domain.user.application.exception

import com.weeth.global.common.exception.BaseException

class UserExistsException : BaseException(UserErrorCode.USER_EXISTS)
