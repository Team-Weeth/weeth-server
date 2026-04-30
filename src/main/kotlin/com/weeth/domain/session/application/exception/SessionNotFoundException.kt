package com.weeth.domain.session.application.exception

import com.weeth.global.common.exception.BaseException

class SessionNotFoundException : BaseException(SessionErrorCode.SESSION_NOT_FOUND)
