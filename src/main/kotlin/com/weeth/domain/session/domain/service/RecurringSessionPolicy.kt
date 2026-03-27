package com.weeth.domain.session.domain.service

import com.weeth.domain.session.domain.enums.RecurrenceType
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class RecurringSessionPolicy {
    fun calculateDates(
        startDate: LocalDate,
        recurrenceType: RecurrenceType,
        recurrenceEndDate: LocalDate,
    ): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var index = 0

        while (true) {
            val current =
                when (recurrenceType) {
                    // startDate.plusMonths(n) 방식으로 1/31 → 2/28 → 3/31 대응
                    RecurrenceType.MONTHLY -> startDate.plusMonths(index.toLong())

                    RecurrenceType.WEEKLY -> startDate.plusWeeks(index.toLong())

                    RecurrenceType.DAILY -> startDate.plusDays(index.toLong())
                }
            if (current.isAfter(recurrenceEndDate)) break
            dates.add(current)
            index++
        }

        return dates
    }
}
