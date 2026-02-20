package com.weeth.domain.schedule.application.exception

import com.weeth.global.common.exception.BaseException

class MeetingNotFoundException : BaseException(MeetingErrorCode.MEETING_NOT_FOUND)
