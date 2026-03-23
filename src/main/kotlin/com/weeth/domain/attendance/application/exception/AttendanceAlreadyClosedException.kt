package com.weeth.domain.attendance.application.exception

import com.weeth.global.common.exception.BaseException

class AttendanceAlreadyClosedException : BaseException(AttendanceErrorCode.ATTENDANCE_ALREADY_CLOSED)
