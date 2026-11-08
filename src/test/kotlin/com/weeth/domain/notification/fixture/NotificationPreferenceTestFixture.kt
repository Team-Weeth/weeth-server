package com.weeth.domain.notification.fixture

import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.enums.Status
import com.weeth.domain.user.domain.vo.Email
import java.util.concurrent.atomic.AtomicInteger

object NotificationPreferenceTestFixture {
    private val sequence = AtomicInteger()

    fun createClub() =
        sequence.incrementAndGet().let { ClubTestFixture.createClub(name = "알림동아리$it", code = "PREF$it") }

    fun createUser() =
        sequence.incrementAndGet().let {
            User(name = "알림회원$it", email = Email.from("pref$it@test.com"), status = Status.ACTIVE)
        }
}
