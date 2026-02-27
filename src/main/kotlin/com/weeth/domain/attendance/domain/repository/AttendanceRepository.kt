package com.weeth.domain.attendance.domain.repository

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.session.domain.entity.Session
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.entity.enums.Status
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface AttendanceRepository : JpaRepository<Attendance, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
    @Query("SELECT a FROM Attendance a JOIN FETCH a.user WHERE a.session = :session AND a.user = :user")
    fun findBySessionAndUserWithLock(
        @Param("session") session: Session,
        @Param("user") user: User,
    ): Attendance?

    @EntityGraph(attributePaths = ["user"])
    fun findAllBySessionAndUserStatus(
        session: Session,
        status: Status,
    ): List<Attendance>

    @Query("SELECT a FROM Attendance a JOIN FETCH a.user WHERE a.id = :id")
    fun findByIdWithUser(id: Long): Attendance?

    @Query(
        """
        SELECT a FROM Attendance a
        JOIN FETCH a.session s
        WHERE a.user.id = :userId
        AND s.start <= :checkInEnd
        AND s.end > :now
        """,
    )
    fun findCurrentByUserId(
        @Param("userId") userId: Long,
        @Param("now") now: LocalDateTime,
        @Param("checkInEnd") checkInEnd: LocalDateTime,
    ): Attendance?

    @Query(
        """
        SELECT a FROM Attendance a
        JOIN FETCH a.session s
        WHERE a.user.id = :userId
        AND s.start >= :dayStart
        AND s.end < :dayEnd
        """,
    )
    fun findTodayByUserId(
        @Param("userId") userId: Long,
        @Param("dayStart") dayStart: LocalDateTime,
        @Param("dayEnd") dayEnd: LocalDateTime,
    ): Attendance?

    @Query(
        """
        SELECT a FROM Attendance a
        JOIN FETCH a.session s
        WHERE a.user.id = :userId
        AND s.cardinal = :cardinal
        ORDER BY s.start
        """,
    )
    fun findAllByUserIdAndCardinal(
        @Param("userId") userId: Long,
        @Param("cardinal") cardinal: Int,
    ): List<Attendance>

    // TODO: QR 코드 출석 기능 구현 시 사용 예정 (여러 세션의 출석자 배치 조회)
    @Query("SELECT a FROM Attendance a JOIN FETCH a.user WHERE a.session IN :sessions")
    fun findAllBySessionIn(
        @Param("sessions") sessions: List<Session>,
    ): List<Attendance>

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Attendance a WHERE a.session = :session")
    fun deleteAllBySession(session: Session)
}
