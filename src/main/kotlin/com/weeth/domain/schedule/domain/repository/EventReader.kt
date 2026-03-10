package com.weeth.domain.schedule.domain.repository

import com.weeth.domain.schedule.domain.entity.Event
import java.time.LocalDateTime

interface EventReader {
    fun findByDateRange(
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<Event>

    fun findAllByCardinal(cardinal: Int): List<Event>
}
