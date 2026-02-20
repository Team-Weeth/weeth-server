package com.weeth.domain.attendance.domain.repository

import com.weeth.domain.attendance.domain.entity.Session
import java.time.LocalDateTime

// TODO: QR 코드 출석 기능 구현 시 사용 예정 (현재 시간 기준 진행 중인 세션 조회)
interface SessionReader {
    fun findAllByStartBetween(
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<Session>
}
