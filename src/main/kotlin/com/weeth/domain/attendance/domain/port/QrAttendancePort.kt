package com.weeth.domain.attendance.domain.port

interface QrAttendancePort {
    fun store(
        code: Int,
        sessionId: Long,
    )

    fun getSessionId(code: Int): Long?
}
