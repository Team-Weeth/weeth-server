package com.weeth.domain.attendance.domain.port

interface QrAttendancePort {
    /**
     * QR 출석 코드를 Redis에 저장합니다.
     * key: sessionId, value: code (TTL 10분)
     */
    fun store(
        sessionId: Long,
        code: Int,
    )

    /**
     * sessionId에 해당하는 활성화된 QR 코드를 반환합니다.
     * QR이 생성된 적 없거나 TTL이 만료된 경우 null을 반환합니다.
     */
    fun getCode(sessionId: Long): Int?
}
