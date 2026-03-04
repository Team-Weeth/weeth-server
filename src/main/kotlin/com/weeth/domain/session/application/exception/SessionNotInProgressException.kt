package com.weeth.domain.session.application.exception

import com.weeth.global.common.exception.BaseException

class SessionNotInProgressException : BaseException(SessionErrorCode.SESSION_NOT_IN_PROGRESS)
