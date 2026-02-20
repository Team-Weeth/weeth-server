package com.weeth.domain.user.application.exception

import com.weeth.global.common.exception.BaseException

class UserNotFoundException : BaseException(UserErrorCode.USER_NOT_FOUND)
