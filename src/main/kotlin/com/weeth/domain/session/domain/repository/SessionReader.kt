package com.weeth.domain.session.domain.repository

import com.weeth.domain.session.domain.entity.Session
import com.weeth.domain.session.domain.entity.enums.SessionStatus
import java.time.LocalDateTime

interface SessionReader {
    fun getById(sessionId: Long): Session

    // TODO: QR 코드 출석 기능 구현 시 사용 예정 (현재 시간 기준 진행 중인 세션 조회)
    fun findAllByStartBetween(
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<Session>

    fun findByStartLessThanEqualAndEndGreaterThanEqualOrderByStartAsc(
        end: LocalDateTime,
        start: LocalDateTime,
    ): List<Session>

    fun findAllByCardinal(cardinal: Int): List<Session>

    fun findAllByCardinalIn(cardinals: List<Int>): List<Session>

    fun findAllByCardinalOrderByStartAsc(cardinal: Int): List<Session>

    fun findAllByStatusAndEndBeforeOrderByEndAsc(
        status: SessionStatus,
        end: LocalDateTime,
    ): List<Session>
}
