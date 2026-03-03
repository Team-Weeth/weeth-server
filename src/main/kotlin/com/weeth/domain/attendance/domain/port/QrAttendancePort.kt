package com.weeth.domain.attendance.domain.port

interface QrAttendancePort {
    fun store(
        sessionId: Long,
        code: Int,
    )

    fun getCode(sessionId: Long): Int?
}
