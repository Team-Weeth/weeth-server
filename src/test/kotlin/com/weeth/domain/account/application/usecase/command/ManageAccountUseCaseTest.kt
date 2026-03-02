package com.weeth.domain.account.application.usecase.command

import com.weeth.domain.account.application.dto.request.AccountSaveRequest
import com.weeth.domain.account.application.exception.AccountExistsException
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.user.fixture.CardinalTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class ManageAccountUseCaseTest :
    DescribeSpec({
        val accountRepository = mockk<AccountRepository>(relaxed = true)
        val cardinalReader = mockk<CardinalReader>(relaxed = true)
        val useCase = ManageAccountUseCase(accountRepository, cardinalReader)

        beforeTest {
            clearMocks(accountRepository, cardinalReader)
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
                    every { cardinalReader.getByCardinalNumber(40) } returns
                        CardinalTestFixture.createCardinal(cardinalNumber = 40, year = 2026, semester = 1)
                    every { accountRepository.save(any()) } answers { firstArg() }

                    useCase.save(request)

                    verify(exactly = 1) { cardinalReader.getByCardinalNumber(40) }
                    verify(exactly = 1) { accountRepository.save(any()) }
                }
            }
        }
    })
