package com.weeth.domain.schedule.application.usecase

import com.weeth.domain.schedule.application.dto.ScheduleDTO
import com.weeth.domain.schedule.domain.service.EventGetService
import com.weeth.domain.schedule.domain.service.MeetingGetService
import com.weeth.domain.user.domain.service.CardinalGetService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime

class ScheduleUseCaseImplTest :
    DescribeSpec({
        val eventGetService = mockk<EventGetService>()
        val meetingGetService = mockk<MeetingGetService>()
        val cardinalGetService = mockk<CardinalGetService>()
        val useCase = ScheduleUseCaseImpl(eventGetService, meetingGetService, cardinalGetService)

        beforeTest {
            clearMocks(eventGetService, meetingGetService, cardinalGetService)
        }

        describe("findByMonthly") {
            val start = LocalDateTime.of(2026, 3, 1, 0, 0)
            val end = LocalDateTime.of(2026, 3, 31, 23, 59)

            it("Event와 Meeting을 합쳐서 start 기준 오름차순 정렬한다") {
                val event1 = ScheduleDTO.Response(1L, "Event", start.plusDays(2), start.plusDays(3), false)
                val meeting1 = ScheduleDTO.Response(2L, "Meeting", start.plusDays(1), start.plusDays(1), true)

                every { eventGetService.find(start, end) } returns listOf(event1)
                every { meetingGetService.find(start, end) } returns listOf(meeting1)

                val result = useCase.findByMonthly(start, end)

                result.size shouldBe 2
                result[0].title() shouldBe "Meeting"
                result[1].title() shouldBe "Event"
            }

            it("Event와 Meeting 모두 없으면 빈 리스트를 반환한다") {
                every { eventGetService.find(start, end) } returns emptyList()
                every { meetingGetService.find(start, end) } returns emptyList()

                val result = useCase.findByMonthly(start, end)

                result.size shouldBe 0
            }
        }
    })
