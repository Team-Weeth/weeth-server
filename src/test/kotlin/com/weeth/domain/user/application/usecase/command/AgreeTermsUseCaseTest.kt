package com.weeth.domain.user.application.usecase.command

import com.weeth.domain.user.application.dto.request.AgreeTermsRequest
import com.weeth.domain.user.domain.repository.UserRepository
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk

class AgreeTermsUseCaseTest :
    DescribeSpec({
        val userRepository = mockk<UserRepository>()
        val useCase = AgreeTermsUseCase(userRepository)

        beforeTest { clearMocks(userRepository) }

        describe("execute") {
            context("모든 약관에 동의한 경우") {
                it("약관 동의 상태를 true로 변경한다") {
                    val user = UserTestFixture.createActiveUser1(1L)
                    every { userRepository.getById(1L) } returns user

                    useCase.execute(1L, AgreeTermsRequest(termsAgreed = true, privacyAgreed = true))

                    user.termsAgreed shouldBe true
                    user.privacyAgreed shouldBe true
                }
            }

            context("약관에 동의하지 않은 경우") {
                it("termsAgreed가 false이면 예외가 발생한다") {
                    val user = UserTestFixture.createActiveUser1(1L)
                    every { userRepository.getById(1L) } returns user

                    shouldThrow<IllegalArgumentException> {
                        useCase.execute(1L, AgreeTermsRequest(termsAgreed = false, privacyAgreed = true))
                    }
                }

                it("privacyAgreed가 false이면 예외가 발생한다") {
                    val user = UserTestFixture.createActiveUser1(1L)
                    every { userRepository.getById(1L) } returns user

                    shouldThrow<IllegalArgumentException> {
                        useCase.execute(1L, AgreeTermsRequest(termsAgreed = true, privacyAgreed = false))
                    }
                }
            }
        }
    })
