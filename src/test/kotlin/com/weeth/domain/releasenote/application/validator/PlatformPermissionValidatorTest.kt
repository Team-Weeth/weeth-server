package com.weeth.domain.releasenote.application.validator

import com.weeth.domain.releasenote.application.exception.NotDeveloperException
import com.weeth.domain.user.domain.enums.PlatformRole
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import org.springframework.test.util.ReflectionTestUtils

class PlatformPermissionValidatorTest :
    DescribeSpec({
        val userReader = mockk<UserReader>()
        val validator = PlatformPermissionValidator(userReader)

        val userId = 10L

        describe("requireDeveloper") {
            it("DEVELOPER 권한이면 통과한다") {
                val developer =
                    UserTestFixture.createActiveUser1(userId).apply {
                        ReflectionTestUtils.setField(this, "platformRole", PlatformRole.DEVELOPER)
                    }
                every { userReader.getById(userId) } returns developer

                shouldNotThrowAny { validator.requireDeveloper(userId) }
            }

            it("platformRole이 null이면 NotDeveloperException(403)을 던진다") {
                every { userReader.getById(userId) } returns UserTestFixture.createActiveUser1(userId)

                shouldThrow<NotDeveloperException> { validator.requireDeveloper(userId) }
            }
        }
    })
