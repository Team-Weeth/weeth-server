package com.weeth.domain.attendance.domain.repository

import com.weeth.domain.attendance.domain.entity.Session
import com.weeth.domain.attendance.domain.entity.enums.SessionStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface SessionRepository :
    JpaRepository<Session, Long>,
    SessionReader {
    fun findByStartLessThanEqualAndEndGreaterThanEqualOrderByStartAsc(
        end: LocalDateTime,
        start: LocalDateTime,
    ): List<Session>

    fun findAllByCardinalOrderByStartAsc(cardinal: Int): List<Session>

    fun findAllByCardinalOrderByStartDesc(cardinal: Int): List<Session>

    fun findAllByCardinal(cardinal: Int): List<Session>

    fun findAllByStatusAndEndBeforeOrderByEndAsc(
        status: SessionStatus,
        end: LocalDateTime,
    ): List<Session>

    fun findAllByOrderByStartDesc(): List<Session>
}
