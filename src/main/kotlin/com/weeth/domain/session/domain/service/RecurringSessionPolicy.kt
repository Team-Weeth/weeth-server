package com.weeth.domain.session.domain.service

import com.weeth.domain.session.domain.enums.RecurrenceType
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Service
class RecurringSessionPolicy {
    fun adjustTime(
        originalStart: LocalDateTime,
        newStart: LocalDateTime,
        newEnd: LocalDateTime,
    ): Pair<LocalDateTime, LocalDateTime> {
        val startTime = newStart.toLocalTime()
        val duration = Duration.between(newStart, newEnd)
        val start = LocalDateTime.of(originalStart.toLocalDate(), startTime)

        return start to start.plus(duration)
    }

    /**
     * 반복 세션의 시작/종료 시각 쌍을 계산한다.
     */
    fun calculateSchedules(
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        recurrenceType: RecurrenceType,
        recurrenceEndDate: LocalDate,
    ): List<Pair<LocalDateTime, LocalDateTime>> {
        val startDate = startDateTime.toLocalDate()
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

            val base = LocalDateTime.of(currentDate, startDateTime.toLocalTime())
            val (start, end) = adjustTime(base, startDateTime, endDateTime)
            schedules.add(start to end)
            index++
        }

        return schedules
    }

    /**
     * 반복 유형과 기준 날짜로 사람이 읽을 수 있는 설명 문자열을 생성한다.
     * ex) "매일 14시", "매주 수요일 14시", "매월 15일 14시"
     */
    fun buildRecurrenceDescription(
        recurrenceType: RecurrenceType,
        startTime: LocalTime,
        baseDate: LocalDate,
    ): String {
        val timeStr = startTime.format(DateTimeFormatter.ofPattern("H시"))
        return when (recurrenceType) {
            RecurrenceType.DAILY -> {
                "매일 $timeStr"
            }

            RecurrenceType.WEEKLY -> {
                val dayOfWeek =
                    when (baseDate.dayOfWeek.value) {
                        1 -> "월요일"
                        2 -> "화요일"
                        3 -> "수요일"
                        4 -> "목요일"
                        5 -> "금요일"
                        6 -> "토요일"
                        else -> "일요일"
                    }
                "매주 $dayOfWeek $timeStr"
            }

            RecurrenceType.MONTHLY -> {
                "매월 ${baseDate.dayOfMonth}일 $timeStr"
            }
        }
    }
}
