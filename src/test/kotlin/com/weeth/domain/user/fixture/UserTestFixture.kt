package com.weeth.domain.user.fixture

import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.enums.Status
import com.weeth.domain.user.domain.vo.Email
import org.springframework.test.util.ReflectionTestUtils

object UserTestFixture {
    fun createActiveUser1(id: Long = 0L): User =
        User(
            name = "적순",
            email = Email.from("test1@test.com"),
            status = Status.ACTIVE,
        ).applyId(id)

    fun createActiveUser2(id: Long = 0L): User =
        User(
            name = "적순2",
            email = Email.from("test2@test.com"),
            status = Status.ACTIVE,
        ).applyId(id)

    fun createWaitingUser1(id: Long = 0L): User =
        User(
            name = "순적",
            email = Email.from("test2@test.com"),
            status = Status.WAITING,
        ).applyId(id)

    fun createWaitingUser2(id: Long = 0L): User =
        User(
            name = "순적2",
            email = Email.from("test3@test.com"),
            status = Status.WAITING,
        ).applyId(id)

    fun createRegisteredUser(id: Long = 0L): User =
        User(
            name = "등록완료",
            email = Email.from("registered@test.com"),
            status = Status.ACTIVE,
        ).apply {
            agreeTerms(termsAgreed = true, privacyAgreed = true)
        }.applyId(id)

    fun createAdmin(id: Long = 0L): User =
        User(
            name = "적순",
            email = Email.from("admin@test.com"),
            status = Status.ACTIVE,
        ).applyId(id)

    private fun User.applyId(id: Long): User =
        apply {
            if (id != 0L) ReflectionTestUtils.setField(this, "id", id)
        }
}
