package com.weeth.domain.attendance.domain.repository

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.schedule.domain.entity.Meeting
import com.weeth.domain.user.domain.entity.enums.Status
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface AttendanceRepository : JpaRepository<Attendance, Long> {
    @EntityGraph(attributePaths = ["user"])
    fun findAllByMeetingAndUserStatus(
        meeting: Meeting,
        status: Status,
    ): List<Attendance>

    @Query("SELECT a FROM Attendance a JOIN FETCH a.user WHERE a.id = :id")
    fun findByIdWithUser(id: Long): Attendance?

    @Query(
        """
        SELECT a FROM Attendance a
        JOIN FETCH a.meeting m
        WHERE a.user.id = :userId
        AND m.start <= :checkInEnd
        AND m.end > :now
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
        JOIN FETCH a.meeting m
        WHERE a.user.id = :userId
        AND m.start >= :dayStart
        AND m.end < :dayEnd
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
        JOIN FETCH a.meeting m
        WHERE a.user.id = :userId
        AND m.cardinal = :cardinal
        ORDER BY m.start
        """,
    )
    fun findAllByUserIdAndCardinal(
        @Param("userId") userId: Long,
        @Param("cardinal") cardinal: Int,
    ): List<Attendance>

    @Modifying
    @Query("DELETE FROM Attendance a WHERE a.meeting = :meeting")
    fun deleteAllByMeeting(meeting: Meeting)
}
