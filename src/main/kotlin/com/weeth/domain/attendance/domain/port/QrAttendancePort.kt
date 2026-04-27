package com.weeth.domain.attendance.domain.port

import java.time.LocalDateTime

interface QrAttendancePort {
    companion object {
        const val TTL_SECONDS = 600L
        const val KEY_PREFIX = "qr:"
    }

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

    /**
     * sessionId에 해당하는 QR 코드의 만료 시각을 반환합니다.
     * QR이 없거나 TTL이 만료된 경우 null을 반환합니다.
     */
    fun getExpiredAt(sessionId: Long): LocalDateTime?
}
