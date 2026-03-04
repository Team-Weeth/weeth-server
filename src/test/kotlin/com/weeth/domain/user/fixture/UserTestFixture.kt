package com.weeth.domain.user.fixture

import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.enums.Role
import com.weeth.domain.user.domain.enums.Status
import com.weeth.domain.user.domain.vo.Email

object UserTestFixture {
    fun createActiveUser1(id: Long? = null): User =
        User(
            id = id ?: 0L,
            name = "적순",
            email = Email.from("test1@test.com"),
            status = Status.ACTIVE,
        )

    fun createActiveUser2(id: Long? = null): User =
        User(
            id = id ?: 0L,
            name = "적순2",
            email = Email.from("test2@test.com"),
            status = Status.ACTIVE,
        )

    fun createWaitingUser1(id: Long? = null): User =
        User(
            id = id ?: 0L,
            name = "순적",
            email = Email.from("test2@test.com"),
            status = Status.WAITING,
        )

    fun createWaitingUser2(id: Long? = null): User =
        User(
            id = id ?: 0L,
            name = "순적2",
            email = Email.from("test3@test.com"),
            status = Status.WAITING,
        )

    fun createAdmin(id: Long? = null): User =
        User(
            id = id ?: 0L,
            name = "적순",
            email = Email.from("admin@test.com"),
            status = Status.ACTIVE,
            role = Role.ADMIN,
        )
}
