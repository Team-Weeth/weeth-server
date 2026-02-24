package com.weeth.domain.schedule.application.exception

import com.weeth.global.common.exception.BaseException

class SessionNotFoundException : BaseException(SessionErrorCode.SESSION_NOT_FOUND)
