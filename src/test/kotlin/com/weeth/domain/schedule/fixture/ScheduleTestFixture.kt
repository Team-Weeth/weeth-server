package com.weeth.domain.schedule.fixture

import com.weeth.domain.schedule.domain.entity.Event
import com.weeth.domain.schedule.domain.entity.Meeting
import java.time.LocalDateTime

object ScheduleTestFixture {
    fun createEvent(): Event =
        Event
            .builder()
            .title("Test Meeting")
            .location("Test Location")
            .start(LocalDateTime.now())
            .end(LocalDateTime.now().plusDays(2))
            .cardinal(1)
            .build()

    fun createMeeting(): Meeting =
        Meeting
            .builder()
            .title("Test Meeting")
            .location("Test Location")
            .start(LocalDateTime.now())
            .end(LocalDateTime.now().plusDays(2))
            .code(1234)
            .cardinal(1)
            .build()
}
