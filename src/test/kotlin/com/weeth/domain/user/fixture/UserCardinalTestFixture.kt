package com.weeth.domain.user.fixture

import com.weeth.domain.user.domain.entity.Cardinal
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.entity.UserCardinal

object UserCardinalTestFixture {
    fun linkUserCardinal(
        user: User,
        cardinal: Cardinal,
    ): UserCardinal = UserCardinal(user, cardinal)
}
