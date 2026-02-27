package com.weeth.domain.user.application.exception

import com.weeth.global.common.exception.BaseException

class EmailNotFoundException : BaseException(UserErrorCode.EMAIL_NOT_FOUND)
