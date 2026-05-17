package com.weeth.domain.attendance.domain.port

import java.time.LocalDateTime

interface QrAttendancePort {
    companion object {
        const val TTL_SECONDS = 600L
        const val ACTIVE_TTL_SECONDS = TTL_SECONDS + 30L
        const val KEY_PREFIX = "qr:"
        const val ACTIVE_KEY_PREFIX = "active-qr:"
    }

    /**
     * QR 출석 코드와 클럽의 현재 활성 QR 세션을 Redis에 저장합니다.
     * code key: sessionId, value: code (TTL 10분)
     * active key: clubId, value: sessionId (TTL 10분 30초)
     */
    fun store(
        clubId: Long,
        sessionId: Long,
        code: Int,
    )

    /**
     * sessionId에 해당하는 활성화된 QR 코드를 반환합니다.
     * QR이 생성된 적 없거나 TTL이 만료된 경우 null을 반환합니다.
     */
    fun getCode(sessionId: Long): Int?

    /**
     * clubId에 해당하는 현재 활성 QR 세션 ID를 반환합니다.
     * 활성 QR이 없거나 active key가 만료된 경우 null을 반환합니다.
     */
    fun getActiveSessionId(clubId: Long): Long?

    /**
     * 현재 활성 QR 세션이 sessionId와 일치하면 active key를 삭제하고 true를 반환합니다.
     * 일치하지 않거나 활성 QR이 없으면 false를 반환합니다.
     */
    fun clearActiveSessionIfMatches(
        clubId: Long,
        sessionId: Long,
    ): Boolean

    /**
     * sessionId에 해당하는 QR 코드의 만료 시각을 반환합니다.
     * QR이 없거나 TTL이 만료된 경우 null을 반환합니다.
     */
    fun getExpiredAt(sessionId: Long): LocalDateTime?
}
