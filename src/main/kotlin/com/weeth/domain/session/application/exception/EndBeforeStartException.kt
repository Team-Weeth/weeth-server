package com.weeth.domain.session.application.exception

import com.weeth.global.common.exception.BaseException

class EndBeforeStartException : BaseException(SessionErrorCode.END_BEFORE_START)
