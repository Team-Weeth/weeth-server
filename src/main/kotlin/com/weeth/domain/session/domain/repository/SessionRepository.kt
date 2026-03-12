package com.weeth.domain.session.domain.repository

import com.weeth.domain.session.application.exception.SessionNotFoundException
import com.weeth.domain.session.domain.entity.Session
import com.weeth.domain.session.domain.enums.SessionStatus
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

    fun findByIdAndClubId(
        sessionId: Long,
        clubId: Long,
    ): Session?

    fun findAllByClubIdOrderByStartDesc(clubId: Long): List<Session>

    fun findAllByClubIdAndCardinalOrderByStartDesc(
        clubId: Long,
        cardinal: Int,
    ): List<Session>

    override fun findAllByCardinalOrderByStartAsc(cardinal: Int): List<Session>

    override fun findAllByCardinal(cardinal: Int): List<Session>

    override fun findAllByStatusAndEndBeforeOrderByEndAsc(
        status: SessionStatus,
        end: LocalDateTime,
    ): List<Session>

    override fun getById(sessionId: Long): Session = findById(sessionId).orElseThrow { SessionNotFoundException() }

    @Query("SELECT s FROM Session s WHERE s.club.id = :clubId AND s.start <= :end AND s.end >= :start")
    override fun findAllByClubIdAndStartBetween(
        @Param("clubId") clubId: Long,
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime,
    ): List<Session>

    @Query("SELECT s FROM Session s WHERE s.club.id = :clubId AND s.cardinal IN :cardinals")
    fun findByStartLessThanEqualAndEndGreaterThanEqualOrderByStartAsc(
        end: LocalDateTime,
        start: LocalDateTime,
    ): List<Session>

    override fun findByDateRange(
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<Session> = findByStartLessThanEqualAndEndGreaterThanEqualOrderByStartAsc(end, start)

    override fun findAllByClubIdAndCardinalIn(
        @Param("clubId") clubId: Long,
        @Param("cardinals") cardinals: List<Int>,
    ): List<Session>
}
