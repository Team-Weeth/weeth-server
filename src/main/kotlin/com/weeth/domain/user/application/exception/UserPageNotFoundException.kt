package com.weeth.domain.user.application.exception

import com.weeth.global.common.exception.BaseException

class UserPageNotFoundException : BaseException(UserErrorCode.USER_PAGE_NOT_FOUND)
