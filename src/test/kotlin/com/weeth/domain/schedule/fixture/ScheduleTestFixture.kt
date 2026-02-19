package com.weeth.domain.schedule.fixture

import com.weeth.domain.attendance.domain.entity.Session
import com.weeth.domain.attendance.domain.entity.enums.SessionStatus
import com.weeth.domain.schedule.domain.entity.Event
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
            Event.create(
                title = title,
                content = content,
                location = location,
                cardinal = cardinal,
                requiredItem = null,
                start = start,
                end = end,
                user = null,
            )
        if (id != 0L) ReflectionTestUtils.setField(event, "id", id)
        return event
    }

    fun createSession(
        id: Long = 0L,
        title: String = "Test Session",
        content: String = "Test Content",
        location: String = "Test Location",
        cardinal: Int = 1,
        code: Int = 1234,
        status: SessionStatus = SessionStatus.OPEN,
        start: LocalDateTime = LocalDateTime.of(2026, 3, 1, 10, 0),
        end: LocalDateTime = LocalDateTime.of(2026, 3, 1, 12, 0),
    ): Session {
        val session =
            Session(
                title = title,
                content = content,
                location = location,
                cardinal = cardinal,
                code = code,
                status = status,
                start = start,
                end = end,
            )
        if (id != 0L) ReflectionTestUtils.setField(session, "id", id)
        return session
    }
}
