package com.weeth.domain.schedule.domain.repository

import com.weeth.domain.schedule.domain.entity.Event
import org.springframework.data.jpa.repository.JpaRepository
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
}
