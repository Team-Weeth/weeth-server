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

    @Query(
        """
        SELECT e
        FROM Event e
        WHERE e.start <= :end
          AND e.end >= :start
        ORDER BY e.start ASC
        """,
    )
    override fun findByDateRange(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime,
    ): List<Event>

    override fun findAllByCardinal(cardinal: Int): List<Event>
}
