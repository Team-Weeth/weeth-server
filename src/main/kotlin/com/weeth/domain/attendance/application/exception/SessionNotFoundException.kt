package com.weeth.domain.attendance.application.exception

import com.weeth.global.common.exception.BaseException

class SessionNotFoundException : BaseException(SessionErrorCode.SESSION_NOT_FOUND)
