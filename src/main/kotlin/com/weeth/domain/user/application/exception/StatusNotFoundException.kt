package com.weeth.domain.user.application.exception

import com.weeth.global.common.exception.BaseException

class StatusNotFoundException : BaseException(UserErrorCode.STATUS_NOT_FOUND)
