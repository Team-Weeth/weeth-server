package com.weeth.domain.account.application.usecase.query

import com.weeth.domain.account.application.exception.AccountNotFoundException
import com.weeth.domain.account.application.mapper.AccountMapper
import com.weeth.domain.account.application.mapper.ReceiptMapper
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.account.domain.repository.ReceiptRepository
import com.weeth.domain.account.fixture.AccountTestFixture
import com.weeth.domain.account.fixture.ReceiptTestFixture
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.entity.FileOwnerType
import com.weeth.domain.file.domain.repository.FileReader
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class GetAccountQueryServiceTest :
    DescribeSpec({
        val accountRepository = mockk<AccountRepository>()
        val receiptRepository = mockk<ReceiptRepository>()
        val fileReader = mockk<FileReader>()
        val accountMapper = mockk<AccountMapper>()
        val receiptMapper = mockk<ReceiptMapper>()
        val fileMapper = mockk<FileMapper>()
        val queryService =
            GetAccountQueryService(
                accountRepository,
                receiptRepository,
                fileReader,
                accountMapper,
                receiptMapper,
                fileMapper,
            )

        beforeTest {
            clearMocks(accountRepository, receiptRepository, fileReader, accountMapper, receiptMapper, fileMapper)
        }

        describe("findByCardinal") {
            context("존재하는 기수 조회 시") {
                it("영수증이 있으면 fileReader.findAll을 receiptIds 배치로 1회 호출한다") {
                    val account = AccountTestFixture.createAccount(cardinal = 40)
                    val receipt1 = ReceiptTestFixture.createReceipt(id = 1L, account = account)
                    val receipt2 = ReceiptTestFixture.createReceipt(id = 2L, account = account)
                    val accountResponse = mockk<com.weeth.domain.account.application.dto.response.AccountResponse>()

                    every { accountRepository.findByCardinal(40) } returns account
                    every { receiptRepository.findAllByAccountIdOrderByCreatedAtDesc(account.id) } returns
                        listOf(receipt1, receipt2)
                    every { fileReader.findAll(FileOwnerType.RECEIPT, listOf(1L, 2L), null) } returns emptyList()
                    every { fileMapper.toFileResponse(any()) } returns mockk()
                    every { receiptMapper.toResponses(any(), any()) } returns emptyList()
                    every { accountMapper.toResponse(account, emptyList()) } returns accountResponse

                    val result = queryService.findByCardinal(40)

                    result shouldBe accountResponse
                    verify(exactly = 1) { fileReader.findAll(FileOwnerType.RECEIPT, listOf(1L, 2L), null) }
                }

                it("영수증이 없으면 fileReader.findAll을 빈 리스트로 호출한다") {
                    val account = AccountTestFixture.createAccount(cardinal = 40)
                    val accountResponse = mockk<com.weeth.domain.account.application.dto.response.AccountResponse>()

                    every { accountRepository.findByCardinal(40) } returns account
                    every { receiptRepository.findAllByAccountIdOrderByCreatedAtDesc(account.id) } returns emptyList()
                    every { fileReader.findAll(FileOwnerType.RECEIPT, emptyList(), null) } returns emptyList()
                    every { receiptMapper.toResponses(emptyList(), emptyMap()) } returns emptyList()
                    every { accountMapper.toResponse(account, emptyList()) } returns accountResponse

                    queryService.findByCardinal(40)

                    verify(exactly = 1) { fileReader.findAll(FileOwnerType.RECEIPT, emptyList<Long>(), null) }
                }
            }

            context("존재하지 않는 기수 조회 시") {
                it("AccountNotFoundException을 던진다") {
                    every { accountRepository.findByCardinal(99) } returns null

                    shouldThrow<AccountNotFoundException> { queryService.findByCardinal(99) }
                }
            }
        }
    })
