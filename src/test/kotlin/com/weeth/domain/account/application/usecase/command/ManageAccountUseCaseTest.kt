package com.weeth.domain.account.application.usecase.command

import com.weeth.domain.account.application.dto.request.AccountSaveRequest
import com.weeth.domain.account.application.exception.AccountExistsException
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.user.domain.entity.Cardinal
import com.weeth.domain.user.domain.repository.CardinalRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional

class ManageAccountUseCaseTest :
    DescribeSpec({
        val accountRepository = mockk<AccountRepository>(relaxed = true)
        val cardinalRepository = mockk<CardinalRepository>(relaxed = true)
        val useCase = ManageAccountUseCase(accountRepository, cardinalRepository)

        beforeTest {
            clearMocks(accountRepository, cardinalRepository)
        }

        describe("save") {
            context("이미 존재하는 기수로 저장 시") {
                it("AccountExistsException을 던진다") {
                    val request = AccountSaveRequest("설명", 100_000, 40)
                    every { accountRepository.existsByCardinal(40) } returns true

                    shouldThrow<AccountExistsException> { useCase.save(request) }
                }
            }

            context("정상 저장 시") {
                it("기수 존재를 보장하고 account를 저장한다") {
                    val request = AccountSaveRequest("설명", 100_000, 40)
                    every { accountRepository.existsByCardinal(40) } returns false
                    every { cardinalRepository.findByCardinalNumber(40) } returns Optional.of(mockk<Cardinal>())
                    every { accountRepository.save(any()) } answers { firstArg() }

                    useCase.save(request)

                    verify(exactly = 1) { cardinalRepository.findByCardinalNumber(40) }
                    verify(exactly = 0) { cardinalRepository.save(any()) }
                    verify(exactly = 1) { accountRepository.save(any()) }
                }
            }
        }
    })
