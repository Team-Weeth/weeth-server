package com.weeth.domain.user.application.exception

import com.weeth.global.common.exception.BaseException

class UserProfileNotFoundException : BaseException(UserErrorCode.USER_PROFILE_NOT_FOUND)
