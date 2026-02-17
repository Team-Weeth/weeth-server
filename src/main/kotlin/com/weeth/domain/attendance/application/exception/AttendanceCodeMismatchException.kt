package com.weeth.domain.attendance.application.exception

import com.weeth.global.common.exception.BaseException

class AttendanceCodeMismatchException : BaseException(AttendanceErrorCode.ATTENDANCE_CODE_MISMATCH)
