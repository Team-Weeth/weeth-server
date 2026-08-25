package com.weeth.domain.schedule.domain.repository

import com.weeth.domain.schedule.domain.entity.Event
import java.time.LocalDateTime

interface EventReader {
    fun findByDateRange(
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<Event>

    fun findByClubIdAndDateRange(
        clubId: Long,
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<Event>

    fun findByClubIdAndCardinalAndDateRange(
        clubId: Long,
        cardinal: Int,
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<Event>

    fun findAllByCardinal(cardinal: Int): List<Event>
}
