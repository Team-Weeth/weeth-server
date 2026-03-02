package com.weeth.domain.attendance.application.exception

import com.weeth.global.common.exception.BaseException

class QrTokenExpiredException : BaseException(AttendanceErrorCode.QR_TOKEN_EXPIRED)
