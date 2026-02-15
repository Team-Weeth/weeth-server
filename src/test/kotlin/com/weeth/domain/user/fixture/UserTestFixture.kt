package com.weeth.domain.user.fixture

import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.entity.enums.Role
import com.weeth.domain.user.domain.entity.enums.Status

object UserTestFixture {
    fun createActiveUser1(id: Long? = null): User =
        User
            .builder()
            .id(id)
            .name("적순")
            .email("test1@test.com")
            .status(Status.ACTIVE)
            .build()

    fun createActiveUser2(id: Long? = null): User =
        User
            .builder()
            .id(id)
            .name("적순2")
            .email("test2@test.com")
            .status(Status.ACTIVE)
            .build()

    fun createWaitingUser1(id: Long? = null): User =
        User
            .builder()
            .id(id)
            .name("순적")
            .email("test2@test.com")
            .status(Status.WAITING)
            .build()

    fun createWaitingUser2(id: Long? = null): User =
        User
            .builder()
            .id(id)
            .name("순적2")
            .email("test3@test.com")
            .status(Status.WAITING)
            .build()

    fun createAdmin(id: Long? = null): User =
        User
            .builder()
            .id(id)
            .name("적순")
            .email("admin@test.com")
            .status(Status.ACTIVE)
            .role(Role.ADMIN)
            .build()
}
