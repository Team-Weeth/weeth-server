package com.weeth.domain.session.domain.service

import com.weeth.domain.session.domain.enums.RecurrenceType
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class RecurringSessionPolicyTest :
    StringSpec({
        val policy = RecurringSessionPolicy()
        val defaultStartTime = LocalTime.of(14, 0)
        val defaultEndTime = LocalTime.of(16, 0)

        fun startOf(date: LocalDate): LocalDateTime = LocalDateTime.of(date, defaultStartTime)

        fun endOf(date: LocalDate): LocalDateTime = LocalDateTime.of(date, defaultEndTime)

        // === DAILY ===

        "DAILY: 시작일부터 종료일까지 매일 스케줄을 생성한다" {
            val start = LocalDate.of(2026, 3, 1)
            val end = LocalDate.of(2026, 3, 5)

            val schedules = policy.calculateSchedules(startOf(start), endOf(start), RecurrenceType.DAILY, end)

            schedules shouldHaveSize 5
            schedules[0].first shouldBe startOf(LocalDate.of(2026, 3, 1))
            schedules[0].second shouldBe endOf(LocalDate.of(2026, 3, 1))
            schedules[4].first shouldBe startOf(LocalDate.of(2026, 3, 5))
            schedules[4].second shouldBe endOf(LocalDate.of(2026, 3, 5))
        }

        "DAILY: 시작일과 종료일이 같으면 1개만 생성한다" {
            val date = LocalDate.of(2026, 3, 1)

            val schedules = policy.calculateSchedules(startOf(date), endOf(date), RecurrenceType.DAILY, date)

            schedules shouldHaveSize 1
            schedules[0].first shouldBe startOf(date)
        }

        // === WEEKLY ===

        "WEEKLY: 매주 같은 요일에 스케줄을 생성한다" {
            val start = LocalDate.of(2026, 3, 4) // 수요일
            val end = LocalDate.of(2026, 3, 25) // 수요일

            val schedules = policy.calculateSchedules(startOf(start), endOf(start), RecurrenceType.WEEKLY, end)

            schedules shouldHaveSize 4
            schedules[0].first shouldBe startOf(LocalDate.of(2026, 3, 4))
            schedules[1].first shouldBe startOf(LocalDate.of(2026, 3, 11))
            schedules[2].first shouldBe startOf(LocalDate.of(2026, 3, 18))
            schedules[3].first shouldBe startOf(LocalDate.of(2026, 3, 25))
        }

        "WEEKLY: 종료일이 정확히 다음 주 전이면 1개만 생성한다" {
            val start = LocalDate.of(2026, 3, 4) // 수요일
            val end = LocalDate.of(2026, 3, 10) // 화요일 (다음 수요일 전)

            val schedules = policy.calculateSchedules(startOf(start), endOf(start), RecurrenceType.WEEKLY, end)

            schedules shouldHaveSize 1
            schedules[0].first shouldBe startOf(start)
        }

        // === MONTHLY ===

        "MONTHLY: 매월 같은 일자에 스케줄을 생성한다" {
            val start = LocalDate.of(2026, 1, 15)
            val end = LocalDate.of(2026, 4, 15)

            val schedules = policy.calculateSchedules(startOf(start), endOf(start), RecurrenceType.MONTHLY, end)

            schedules shouldHaveSize 4
            schedules[0].first shouldBe startOf(LocalDate.of(2026, 1, 15))
            schedules[1].first shouldBe startOf(LocalDate.of(2026, 2, 15))
            schedules[2].first shouldBe startOf(LocalDate.of(2026, 3, 15))
            schedules[3].first shouldBe startOf(LocalDate.of(2026, 4, 15))
        }

        "MONTHLY: 31일 시작이면 짧은 달은 말일로 조정된다" {
            val start = LocalDate.of(2026, 1, 31)
            val end = LocalDate.of(2026, 4, 30)

            val schedules = policy.calculateSchedules(startOf(start), endOf(start), RecurrenceType.MONTHLY, end)

            // 1/31 → 2/28 → 3/31 → 4/30 (원본 기준 plusMonths)
            schedules shouldHaveSize 4
            schedules[0].first.toLocalDate() shouldBe LocalDate.of(2026, 1, 31)
            schedules[1].first.toLocalDate() shouldBe LocalDate.of(2026, 2, 28)
            schedules[2].first.toLocalDate() shouldBe LocalDate.of(2026, 3, 31)
            schedules[3].first.toLocalDate() shouldBe LocalDate.of(2026, 4, 30)
        }

        "MONTHLY: 체이닝 방식과 다르게 원본 기준으로 계산된다" {
            // 체이닝: 1/31 → 2/28 → 3/28 (X)
            // 원본 기준: 1/31 → 2/28 → 3/31 (O)
            val start = LocalDate.of(2026, 1, 31)
            val end = LocalDate.of(2026, 3, 31)

            val schedules = policy.calculateSchedules(startOf(start), endOf(start), RecurrenceType.MONTHLY, end)

            schedules shouldHaveSize 3
            schedules[2].first.toLocalDate() shouldBe LocalDate.of(2026, 3, 31)
        }

        // === duration (자정 넘김) ===

        "자정을 넘기는 세션도 duration 기반으로 정상 처리된다" {
            val date = LocalDate.of(2026, 3, 1)
            val startDateTime = LocalDateTime.of(date, LocalTime.of(22, 0))
            val endDateTime = LocalDateTime.of(date.plusDays(1), LocalTime.of(2, 0)) // 4시간

            val schedules =
                policy.calculateSchedules(
                    startDateTime,
                    endDateTime,
                    RecurrenceType.DAILY,
                    date.plusDays(2),
                )

            schedules shouldHaveSize 3
            // 첫째 날: 3/1 22:00 ~ 3/2 02:00
            schedules[0].first shouldBe LocalDateTime.of(2026, 3, 1, 22, 0)
            schedules[0].second shouldBe LocalDateTime.of(2026, 3, 2, 2, 0)
            // 둘째 날: 3/2 22:00 ~ 3/3 02:00
            schedules[1].first shouldBe LocalDateTime.of(2026, 3, 2, 22, 0)
            schedules[1].second shouldBe LocalDateTime.of(2026, 3, 3, 2, 0)
        }

        // === 경계 조건 ===

        "종료일이 시작일보다 이전이면 빈 리스트를 반환한다" {
            val start = LocalDate.of(2026, 3, 10)
            val end = LocalDate.of(2026, 3, 1)

            val schedules = policy.calculateSchedules(startOf(start), endOf(start), RecurrenceType.WEEKLY, end)

            schedules.shouldBeEmpty()
        }

        "MONTHLY: 종료일이 다음 달 전이면 시작일만 포함된다" {
            val start = LocalDate.of(2026, 3, 15)
            val end = LocalDate.of(2026, 4, 14)

            val schedules = policy.calculateSchedules(startOf(start), endOf(start), RecurrenceType.MONTHLY, end)

            schedules shouldHaveSize 1
            schedules[0].first shouldBe startOf(start)
        }

        // === buildRecurrenceDescription ===

        "DAILY: '매일 N시' 형식으로 반환한다" {
            val result =
                policy.buildRecurrenceDescription(
                    RecurrenceType.DAILY,
                    LocalTime.of(14, 0),
                    LocalDate.of(2026, 3, 4),
                )
            result shouldBe "매일 14시"
        }

        "WEEKLY: '매주 X요일 N시' 형식으로 반환한다" {
            val result =
                policy.buildRecurrenceDescription(
                    RecurrenceType.WEEKLY,
                    LocalTime.of(10, 0),
                    LocalDate.of(2026, 3, 4), // 수요일
                )
            result shouldBe "매주 수요일 10시"
        }

        "MONTHLY: '매월 N일 N시' 형식으로 반환한다" {
            val result =
                policy.buildRecurrenceDescription(
                    RecurrenceType.MONTHLY,
                    LocalTime.of(19, 0),
                    LocalDate.of(2026, 3, 15),
                )
            result shouldBe "매월 15일 19시"
        }
    })
