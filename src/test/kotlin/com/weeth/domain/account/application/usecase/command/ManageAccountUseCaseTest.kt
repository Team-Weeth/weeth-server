package com.weeth.domain.account.application.usecase.command

import com.weeth.domain.account.application.dto.request.AccountSaveRequest
import com.weeth.domain.account.application.exception.AccountExistsException
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.user.domain.service.CardinalGetService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class ManageAccountUseCaseTest :
    DescribeSpec({
        val accountRepository = mockk<AccountRepository>(relaxed = true)
        val cardinalGetService = mockk<CardinalGetService>(relaxUnitFun = true)
        val useCase = ManageAccountUseCase(accountRepository, cardinalGetService)

        beforeTest {
            clearMocks(accountRepository, cardinalGetService)
        }

        describe("save") {
            context("이미 존재하는 기수로 저장 시") {
                it("AccountExistsException을 던진다") {
                    val dto = AccountSaveRequest("설명", 100_000, 40)
                    every { accountRepository.existsByCardinal(40) } returns true

                    shouldThrow<AccountExistsException> { useCase.save(dto) }
                }
            }

            context("정상 저장 시") {
                it("account가 저장된다") {
                    val dto = AccountSaveRequest("설명", 100_000, 40)
                    every { accountRepository.existsByCardinal(40) } returns false
                    every { cardinalGetService.findByAdminSide(40) } returns mockk()
                    every { accountRepository.save(any()) } answers { firstArg() }

                    useCase.save(dto)

                    verify(exactly = 1) { accountRepository.save(any()) }
                }
            }
        }
    })
