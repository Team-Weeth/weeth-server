package com.weeth.domain.attendance.application.exception

import com.weeth.global.common.exception.BaseException

class AttendanceNotFoundException : BaseException(AttendanceErrorCode.ATTENDANCE_NOT_FOUND)
