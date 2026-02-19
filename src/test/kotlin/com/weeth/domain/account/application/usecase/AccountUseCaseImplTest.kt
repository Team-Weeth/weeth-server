package com.weeth.domain.account.application.usecase

import com.weeth.domain.account.application.dto.AccountDTO
import com.weeth.domain.account.application.dto.ReceiptDTO
import com.weeth.domain.account.application.exception.AccountExistsException
import com.weeth.domain.account.application.exception.AccountNotFoundException
import com.weeth.domain.account.application.mapper.AccountMapper
import com.weeth.domain.account.application.mapper.ReceiptMapper
import com.weeth.domain.account.domain.service.AccountGetService
import com.weeth.domain.account.domain.service.AccountSaveService
import com.weeth.domain.account.domain.service.ReceiptGetService
import com.weeth.domain.account.fixture.AccountTestFixture
import com.weeth.domain.account.fixture.ReceiptTestFixture
import com.weeth.domain.file.application.dto.response.FileResponse
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.entity.FileOwnerType
import com.weeth.domain.file.domain.repository.FileReader
import com.weeth.domain.user.domain.service.CardinalGetService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class AccountUseCaseImplTest :
    DescribeSpec({
        val accountGetService = mockk<AccountGetService>()
        val accountSaveService = mockk<AccountSaveService>(relaxUnitFun = true)
        val receiptGetService = mockk<ReceiptGetService>()
        val fileReader = mockk<FileReader>()
        val cardinalGetService = mockk<CardinalGetService>()
        val accountMapper = mockk<AccountMapper>()
        val receiptMapper = mockk<ReceiptMapper>()
        val fileMapper = mockk<FileMapper>()

        val useCase =
            AccountUseCaseImpl(
                accountGetService,
                accountSaveService,
                receiptGetService,
                fileReader,
                cardinalGetService,
                accountMapper,
                receiptMapper,
                fileMapper,
            )

        beforeTest {
            clearMocks(
                accountGetService,
                accountSaveService,
                receiptGetService,
                fileReader,
                cardinalGetService,
                accountMapper,
                receiptMapper,
                fileMapper,
            )
        }

        describe("find") {
            context("존재하는 기수의 회비 조회 시") {
                it("영수증과 파일 정보가 포함된 AccountResponse를 반환한다") {
                    val account = AccountTestFixture.createAccount(cardinal = 40)
                    val receipt = ReceiptTestFixture.createReceipt(id = 10L, amount = 5_000, account = account)
                    val fileResponse = mockk<FileResponse>()
                    val receiptResponse = mockk<ReceiptDTO.Response>()
                    val accountResponse = mockk<AccountDTO.Response>()

                    every { accountGetService.find(40) } returns account
                    every { receiptGetService.findAllByAccountId(account.id) } returns listOf(receipt)
                    every { fileReader.findAll(FileOwnerType.RECEIPT, receipt.id, null) } returns listOf(mockk())
                    every { fileMapper.toFileResponse(any()) } returns fileResponse
                    every { receiptMapper.to(receipt, listOf(fileResponse)) } returns receiptResponse
                    every { accountMapper.to(account, listOf(receiptResponse)) } returns accountResponse

                    val result = useCase.find(40)

                    result shouldBe accountResponse
                }
            }

            context("존재하지 않는 기수 조회 시") {
                it("AccountNotFoundException을 던진다") {
                    every { accountGetService.find(99) } throws AccountNotFoundException()

                    shouldThrow<AccountNotFoundException> { useCase.find(99) }
                }
            }
        }

        describe("save") {
            context("이미 존재하는 기수로 저장 시") {
                it("AccountExistsException을 던진다") {
                    val dto = AccountDTO.Save("설명", 100_000, 40)
                    every { accountGetService.validate(40) } returns true

                    shouldThrow<AccountExistsException> { useCase.save(dto) }
                }
            }

            context("정상 저장 시") {
                it("account가 저장된다") {
                    val dto = AccountDTO.Save("설명", 100_000, 40)
                    val account = AccountTestFixture.createAccount()
                    every { accountGetService.validate(40) } returns false
                    every { cardinalGetService.findByAdminSide(40) } returns mockk()
                    every { accountMapper.from(dto) } returns account

                    useCase.save(dto)

                    verify(exactly = 1) { accountSaveService.save(account) }
                }
            }
        }
    })
