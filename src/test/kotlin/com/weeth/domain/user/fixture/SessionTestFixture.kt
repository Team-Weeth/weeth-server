package com.weeth.domain.user.fixture

import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.session.domain.entity.Session
import java.time.LocalDateTime

object SessionTestFixture {
    fun createSession(
        cardinalNumber: Int,
        title: String = "테스트 세션",
        start: LocalDateTime = LocalDateTime.of(2025, 3, 1, 14, 0),
        end: LocalDateTime = LocalDateTime.of(2025, 3, 1, 16, 0),
        code: Int = 1234,
    ): Session =
        Session(
            club = ClubTestFixture.createClub(),
            title = title,
            cardinal = cardinalNumber,
            start = start,
            end = end,
            code = code,
        )
}
