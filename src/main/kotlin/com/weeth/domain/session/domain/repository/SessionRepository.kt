package com.weeth.domain.session.domain.repository

import com.weeth.domain.session.application.exception.SessionNotFoundException
import com.weeth.domain.session.domain.entity.Session
import com.weeth.domain.session.domain.entity.SessionGroup
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

    @Query("SELECT s FROM Session s LEFT JOIN FETCH s.sessionGroup WHERE s.club.id = :clubId ORDER BY s.start DESC")
    fun findAllByClubIdOrderByStartDesc(
        @Param("clubId") clubId: Long,
    ): List<Session>

    @Query(
        "SELECT s FROM Session s LEFT JOIN FETCH s.sessionGroup WHERE s.club.id = :clubId AND s.cardinal = :cardinal ORDER BY s.start DESC",
    )
    fun findAllByClubIdAndCardinalOrderByStartDesc(
        @Param("clubId") clubId: Long,
        @Param("cardinal") cardinal: Int,
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

    fun findByStartLessThanEqualAndEndGreaterThanEqualOrderByStartAsc(
        end: LocalDateTime,
        start: LocalDateTime,
    ): List<Session>

    override fun findByDateRange(
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<Session> = findByStartLessThanEqualAndEndGreaterThanEqualOrderByStartAsc(end, start)

    @Query(
        "SELECT s FROM Session s WHERE s.club.id = :clubId AND s.cardinal = :cardinal AND s.start <= :end AND s.end >= :start ORDER BY s.start ASC",
    )
    override fun findAllByClubIdAndCardinalAndStartBetween(
        @Param("clubId") clubId: Long,
        @Param("cardinal") cardinal: Int,
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime,
    ): List<Session>

    override fun findAllByClubIdAndCardinalIn(
        clubId: Long,
        cardinals: List<Int>,
    ): List<Session>

    fun findFirstByClubIdAndStatusOrderByIdAsc(
        clubId: Long,
        status: SessionStatus,
    ): Session?

    // OPEN 세션이 복수인 경우 id가 가장 작은 것 반환
    override fun findOpenByClubId(clubId: Long): Session? =
        findFirstByClubIdAndStatusOrderByIdAsc(clubId, SessionStatus.OPEN)

    @Query("SELECT s.club.id FROM Session s WHERE s.id = :sessionId")
    override fun findClubIdById(
        @Param("sessionId") sessionId: Long,
    ): Long?

    // 기준 시작시각 이후 세션 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
    @Query(
        "SELECT s FROM Session s WHERE s.sessionGroup = :group AND s.start >= :start ORDER BY s.start ASC, s.id ASC",
    )
    fun findAllBySessionGroupAndStartGreaterThanEqualWithLock(
        @Param("group") group: SessionGroup,
        @Param("start") start: LocalDateTime,
    ): List<Session>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
    @Query("SELECT s FROM Session s WHERE s.sessionGroup = :group ORDER BY s.start ASC, s.id ASC")
    fun findAllBySessionGroupWithLock(
        @Param("group") group: SessionGroup,
    ): List<Session>

    // 세션 그룹의 남은 세션 수 조회 (삭제 후 빈 그룹 정리용)
    fun countBySessionGroup(group: SessionGroup): Long
}
