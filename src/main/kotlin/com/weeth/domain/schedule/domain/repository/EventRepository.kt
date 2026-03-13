package com.weeth.domain.schedule.domain.repository

import com.weeth.domain.schedule.domain.entity.Event
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface EventRepository :
    JpaRepository<Event, Long>,
    EventReader {
    fun findByStartLessThanEqualAndEndGreaterThanEqualOrderByStartAsc(
        end: LocalDateTime,
        start: LocalDateTime,
    ): List<Event>

    override fun findByDateRange(
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<Event> = findByStartLessThanEqualAndEndGreaterThanEqualOrderByStartAsc(end, start)

    override fun findAllByCardinal(cardinal: Int): List<Event>

    fun findAllByClubIdAndCardinal(
        clubId: Long,
        cardinal: Int,
    ): List<Event>

    @Query(
        "SELECT e FROM Event e WHERE e.club.id = :clubId AND e.start <= :end AND e.end >= :start ORDER BY e.start ASC",
    )
    fun findByClubIdAndStartLessThanEqualAndEndGreaterThanEqualOrderByStartAsc(
        @Param("clubId") clubId: Long,
        @Param("end") end: LocalDateTime,
        @Param("start") start: LocalDateTime,
    ): List<Event>
}
