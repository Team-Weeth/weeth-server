package com.weeth.domain.session.fixture

import com.weeth.domain.club.domain.entity.Club
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.session.domain.entity.Session
import com.weeth.domain.session.domain.enums.SessionStatus
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate
import java.time.LocalDateTime

object SessionTestFixture {
    fun createSession(
        id: Long = 0L,
        club: Club = ClubTestFixture.createClub(),
        title: String = "Test Session",
        content: String = "Test Content",
        location: String = "Test Location",
        cardinal: Int = 1,
        code: Int = 123456,
        status: SessionStatus = SessionStatus.OPEN,
        start: LocalDateTime = LocalDateTime.of(2026, 3, 1, 10, 0),
        end: LocalDateTime = LocalDateTime.of(2026, 3, 1, 12, 0),
    ): Session {
        val session =
            Session(
                club = club,
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

    fun createOneDaySession(
        date: LocalDate,
        cardinal: Int,
        code: Int,
        title: String,
        club: Club = ClubTestFixture.createClub(),
    ): Session =
        Session(
            club = club,
            title = title,
            location = "Test Location",
            start = date.atTime(10, 0),
            end = date.atTime(12, 0),
            code = code,
            cardinal = cardinal,
        )

    fun createInProgressSession(
        cardinal: Int,
        code: Int,
        title: String,
        club: Club = ClubTestFixture.createClub(),
    ): Session =
        Session(
            club = club,
            title = title,
            location = "Test Location",
            start = LocalDateTime.now().minusMinutes(5),
            end = LocalDateTime.now().plusMinutes(5),
            code = code,
            cardinal = cardinal,
        )
}
