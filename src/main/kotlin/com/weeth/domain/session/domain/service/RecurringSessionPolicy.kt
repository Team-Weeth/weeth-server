package com.weeth.domain.session.domain.service

import com.weeth.domain.session.domain.enums.RecurrenceType
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class RecurringSessionPolicy {
    /**
     * 반복 세션의 시작/종료 시각 쌍을 duration 기반으로 계산한다.
     */
    fun calculateSchedules(
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        recurrenceType: RecurrenceType,
        recurrenceEndDate: LocalDate,
    ): List<Pair<LocalDateTime, LocalDateTime>> {
        val startDate = startDateTime.toLocalDate()
        val startTime = startDateTime.toLocalTime()
        val duration = Duration.between(startDateTime, endDateTime)

        val schedules = mutableListOf<Pair<LocalDateTime, LocalDateTime>>()
        var index = 0

        while (true) {
            val currentDate =
                when (recurrenceType) {
                    // startDate.plusMonths(n) 방식으로 1/31 → 2/28 → 3/31 대응
                    RecurrenceType.MONTHLY -> startDate.plusMonths(index.toLong())

                    RecurrenceType.WEEKLY -> startDate.plusWeeks(index.toLong())

                    RecurrenceType.DAILY -> startDate.plusDays(index.toLong())
                }
            if (currentDate.isAfter(recurrenceEndDate)) break

            val start = LocalDateTime.of(currentDate, startTime)
            schedules.add(start to start.plus(duration))
            index++
        }

        return schedules
    }
}
