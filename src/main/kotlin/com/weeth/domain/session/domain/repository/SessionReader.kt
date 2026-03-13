package com.weeth.domain.session.domain.repository

import com.weeth.domain.session.domain.entity.Session
import com.weeth.domain.session.domain.enums.SessionStatus
import java.time.LocalDateTime

interface SessionReader {
    fun getById(sessionId: Long): Session

    fun findByDateRange(
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<Session>

    fun findAllByCardinal(cardinal: Int): List<Session>

    fun findAllByCardinalOrderByStartAsc(cardinal: Int): List<Session>

    fun findAllByStatusAndEndBeforeOrderByEndAsc(
        status: SessionStatus,
        end: LocalDateTime,
    ): List<Session>

    // TODO: QR 코드 출석 기능 구현 시 사용 예정 (현재 시간 기준 진행 중인 세션 조회)
    fun findAllByClubIdAndStartBetween(
        clubId: Long,
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<Session>

    fun findAllByClubIdAndCardinalIn(
        clubId: Long,
        cardinals: List<Int>,
    ): List<Session>
}
