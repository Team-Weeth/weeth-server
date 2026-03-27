package com.weeth.domain.session.application.exception

import com.weeth.global.common.exception.BaseException

class RecurrenceEndDateBeforeStartException : BaseException(SessionErrorCode.RECURRENCE_END_DATE_BEFORE_START)
