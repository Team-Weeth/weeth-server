package com.weeth.domain.attendance.domain.repository

import com.weeth.domain.attendance.domain.entity.Session
import com.weeth.domain.attendance.domain.entity.enums.SessionStatus
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface SessionRepository :
    JpaRepository<Session, Long>,
    SessionReader {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
    @Query("SELECT s FROM Session s WHERE s.id = :id")
    fun findByIdWithLock(id: Long): Session?

    fun findByStartLessThanEqualAndEndGreaterThanEqualOrderByStartAsc(
        end: LocalDateTime,
        start: LocalDateTime,
    ): List<Session>

    fun findAllByCardinalOrderByStartAsc(cardinal: Int): List<Session>

    fun findAllByCardinalOrderByStartDesc(cardinal: Int): List<Session>

    override fun findAllByCardinal(cardinal: Int): List<Session>

    fun findAllByStatusAndEndBeforeOrderByEndAsc(
        status: SessionStatus,
        end: LocalDateTime,
    ): List<Session>

    fun findAllByOrderByStartDesc(): List<Session>

    @Query("SELECT s FROM Session s WHERE s.cardinal IN :cardinals")
    override fun findAllByCardinalIn(
        @Param("cardinals") cardinals: List<Int>,
    ): List<Session>
}
