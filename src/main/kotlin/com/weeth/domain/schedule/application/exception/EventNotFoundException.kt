package com.weeth.domain.schedule.application.exception

import com.weeth.global.common.exception.BaseException

class EventNotFoundException : BaseException(EventErrorCode.EVENT_NOT_FOUND)
