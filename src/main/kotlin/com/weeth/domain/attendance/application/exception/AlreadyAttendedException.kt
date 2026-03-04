package com.weeth.domain.attendance.application.exception

import com.weeth.global.common.exception.BaseException

class AlreadyAttendedException : BaseException(AttendanceErrorCode.ALREADY_ATTENDED)
