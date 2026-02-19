package com.weeth.domain.schedule.fixture

import com.weeth.domain.schedule.domain.entity.Event
import com.weeth.domain.schedule.domain.entity.Meeting
import com.weeth.domain.schedule.domain.entity.enums.MeetingStatus
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime

object ScheduleTestFixture {
    fun createEvent(
        id: Long = 0L,
        title: String = "Test Event",
        content: String = "Test Content",
        location: String = "Test Location",
        cardinal: Int = 1,
        start: LocalDateTime = LocalDateTime.of(2026, 3, 1, 10, 0),
        end: LocalDateTime = LocalDateTime.of(2026, 3, 1, 12, 0),
    ): Event {
        val event =
            Event
                .builder()
                .title(title)
                .content(content)
                .location(location)
                .cardinal(cardinal)
                .start(start)
                .end(end)
                .build()
        if (id != 0L) ReflectionTestUtils.setField(event, "id", id)
        return event
    }

    fun createMeeting(
        id: Long = 0L,
        title: String = "Test Meeting",
        content: String = "Test Content",
        location: String = "Test Location",
        cardinal: Int = 1,
        code: Int = 1234,
        meetingStatus: MeetingStatus = MeetingStatus.OPEN,
        start: LocalDateTime = LocalDateTime.of(2026, 3, 1, 10, 0),
        end: LocalDateTime = LocalDateTime.of(2026, 3, 1, 12, 0),
    ): Meeting {
        val meeting =
            Meeting
                .builder()
                .title(title)
                .content(content)
                .location(location)
                .cardinal(cardinal)
                .code(code)
                .meetingStatus(meetingStatus)
                .start(start)
                .end(end)
                .build()
        if (id != 0L) ReflectionTestUtils.setField(meeting, "id", id)
        return meeting
    }
}
