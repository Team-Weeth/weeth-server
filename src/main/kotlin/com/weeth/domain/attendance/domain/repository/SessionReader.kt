package com.weeth.domain.attendance.domain.repository

import com.weeth.domain.attendance.domain.entity.Session
import java.time.LocalDateTime

interface SessionReader {
    fun findAllByStartBetween(
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<Session>
}
