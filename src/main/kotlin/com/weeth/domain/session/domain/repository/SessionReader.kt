package com.weeth.domain.session.domain.repository

import com.weeth.domain.session.domain.entity.Session
import com.weeth.domain.session.domain.enums.SessionStatus
import java.time.LocalDateTime

interface SessionReader {
    fun getById(sessionId: Long): Session

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
