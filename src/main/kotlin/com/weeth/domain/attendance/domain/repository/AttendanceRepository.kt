package com.weeth.domain.attendance.domain.repository

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.session.domain.entity.Session
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
    @Query(
        "SELECT a FROM Attendance a JOIN FETCH a.clubMember cm JOIN FETCH cm.user WHERE a.session = :session AND a.clubMember = :clubMember",
    )
    fun findBySessionAndClubMemberWithLock(
        @Param("session") session: Session,
        @Param("clubMember") clubMember: ClubMember,
    ): Attendance?

    @EntityGraph(attributePaths = ["clubMember", "clubMember.user"])
    fun findAllBySessionAndClubMemberMemberStatus(
        session: Session,
        memberStatus: MemberStatus,
    ): List<Attendance>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
    @Query(
        "SELECT a FROM Attendance a JOIN FETCH a.clubMember cm JOIN FETCH cm.user WHERE a.session = :session AND cm.memberStatus = :status",
    )
    fun findAllBySessionAndClubMemberMemberStatusWithLock(
        @Param("session") session: Session,
        @Param("status") status: MemberStatus,
    ): List<Attendance>

    @Query("SELECT a FROM Attendance a JOIN FETCH a.clubMember cm JOIN FETCH cm.user WHERE a.id = :id")
    fun findByIdWithClubMember(id: Long): Attendance?

    @Query(
        """
        SELECT a FROM Attendance a
        JOIN FETCH a.session s
        WHERE a.clubMember.id = :clubMemberId
        AND s.start <= :checkInEnd
        AND s.end > :now
        """,
    )
    fun findCurrentByClubMemberId(
        @Param("clubMemberId") clubMemberId: Long,
        @Param("now") now: LocalDateTime,
        @Param("checkInEnd") checkInEnd: LocalDateTime,
    ): Attendance?

    @Query(
        """
        SELECT a FROM Attendance a
        JOIN FETCH a.session s
        WHERE a.clubMember.id = :clubMemberId
        AND s.start >= :dayStart
        AND s.end < :dayEnd
        """,
    )
    fun findTodayByClubMemberId(
        @Param("clubMemberId") clubMemberId: Long,
        @Param("dayStart") dayStart: LocalDateTime,
        @Param("dayEnd") dayEnd: LocalDateTime,
    ): Attendance?

    @Query(
        """
        SELECT a FROM Attendance a
        JOIN FETCH a.session s
        WHERE a.clubMember.id = :clubMemberId
        AND s.cardinal = :cardinal
        ORDER BY s.start
        """,
    )
    fun findAllByClubMemberIdAndCardinal(
        @Param("clubMemberId") clubMemberId: Long,
        @Param("cardinal") cardinal: Int,
    ): List<Attendance>

    @Query("SELECT a FROM Attendance a JOIN FETCH a.clubMember cm JOIN FETCH cm.user WHERE a.session IN :sessions")
    fun findAllBySessionIn(
        @Param("sessions") sessions: List<Session>,
    ): List<Attendance>

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Attendance a WHERE a.session = :session")
    fun deleteAllBySession(session: Session)
}
