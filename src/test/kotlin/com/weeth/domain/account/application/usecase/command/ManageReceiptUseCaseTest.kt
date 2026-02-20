package com.weeth.domain.account.application.usecase.command

import com.weeth.domain.account.application.dto.request.ReceiptSaveRequest
import com.weeth.domain.account.application.dto.request.ReceiptUpdateRequest
import com.weeth.domain.account.application.exception.AccountNotFoundException
import com.weeth.domain.account.application.exception.ReceiptAccountMismatchException
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.account.domain.repository.ReceiptRepository
import com.weeth.domain.account.domain.vo.Money
import com.weeth.domain.account.fixture.AccountTestFixture
import com.weeth.domain.account.fixture.ReceiptTestFixture
import com.weeth.domain.file.application.dto.request.FileSaveRequest
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.entity.FileOwnerType
import com.weeth.domain.file.domain.repository.FileReader
import com.weeth.domain.file.domain.repository.FileRepository
import com.weeth.domain.user.domain.entity.Cardinal
import com.weeth.domain.user.domain.repository.CardinalRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.util.Optional

class ManageReceiptUseCaseTest :
    DescribeSpec({
        val receiptRepository = mockk<ReceiptRepository>(relaxUnitFun = true)
        val accountRepository = mockk<AccountRepository>()
        val fileReader = mockk<FileReader>()
        val fileRepository = mockk<FileRepository>(relaxed = true)
        val cardinalRepository = mockk<CardinalRepository>(relaxed = true)
        val fileMapper = mockk<FileMapper>()
        val useCase =
            ManageReceiptUseCase(
                receiptRepository,
                accountRepository,
                fileReader,
                fileRepository,
                cardinalRepository,
                fileMapper,
            )

        beforeTest {
            clearMocks(receiptRepository, accountRepository, fileReader, fileRepository, cardinalRepository, fileMapper)
        }

        fun stubExistingCardinal(cardinalNumber: Int) {
            every { cardinalRepository.findByCardinalNumber(cardinalNumber) } returns Optional.of(mockk<Cardinal>())
        }

        describe("save") {
            context("파일이 있는 경우") {
                it("영수증 저장 후 fileRepository.saveAll이 호출된다") {
                    val account = AccountTestFixture.createAccount(cardinal = 40)
                    val savedReceipt = ReceiptTestFixture.createReceipt(id = 10L, amount = 5_000, account = account)
                    val files = listOf(mockk<File>())
                    val request =
                        ReceiptSaveRequest(
                            "간식비",
                            "편의점",
                            5_000,
                            LocalDate.of(2024, 9, 1),
                            40,
                            listOf(FileSaveRequest("receipt.png", "TEMP/2024-09/receipt.png", 200L, "image/png")),
                        )

                    stubExistingCardinal(40)
                    every { accountRepository.findByCardinal(40) } returns account
                    every { receiptRepository.save(any()) } returns savedReceipt
                    every { fileMapper.toFileList(request.files, FileOwnerType.RECEIPT, savedReceipt.id) } returns files

                    useCase.save(request)

                    verify(exactly = 1) { receiptRepository.save(any()) }
                    verify(exactly = 1) { fileRepository.saveAll(files) }
                }
            }

            context("파일이 없는 경우") {
                it("fileRepository.saveAll은 빈 리스트로 호출된다") {
                    val account = AccountTestFixture.createAccount(cardinal = 40)
                    val savedReceipt = ReceiptTestFixture.createReceipt(id = 11L, amount = 3_000, account = account)
                    val request = ReceiptSaveRequest("교통비", "지하철", 3_000, LocalDate.of(2024, 9, 2), 40, emptyList())

                    stubExistingCardinal(40)
                    every { accountRepository.findByCardinal(40) } returns account
                    every { receiptRepository.save(any()) } returns savedReceipt
                    every { fileMapper.toFileList(emptyList(), FileOwnerType.RECEIPT, savedReceipt.id) } returns emptyList()

                    useCase.save(request)

                    verify(exactly = 1) { receiptRepository.save(any()) }
                    verify(exactly = 1) { fileRepository.saveAll(emptyList()) }
                }
            }

            context("존재하지 않는 기수로 저장 시") {
                it("AccountNotFoundException을 던진다") {
                    val request = ReceiptSaveRequest("간식비", "편의점", 5_000, LocalDate.of(2024, 9, 1), 99, null)

                    stubExistingCardinal(99)
                    every { accountRepository.findByCardinal(99) } returns null

                    shouldThrow<AccountNotFoundException> { useCase.save(request) }
                }
            }
        }

        describe("update") {
            it("업데이트 파일이 있으면 기존 파일을 삭제 후 새 파일을 저장한다") {
                val receiptId = 10L
                val account = AccountTestFixture.createAccount(cardinal = 40)
                val receipt = ReceiptTestFixture.createReceipt(id = receiptId, amount = 1_000, account = account)
                account.spend(Money.of(receipt.amount))
                val request =
                    ReceiptUpdateRequest(
                        "desc",
                        "source",
                        2_000,
                        LocalDate.of(2026, 1, 1),
                        40,
                        listOf(FileSaveRequest("new.png", "TEMP/2026-02/new.png", 100L, "image/png")),
                    )
                val oldFiles = listOf(mockk<File>())
                val newFiles = listOf(mockk<File>())

                stubExistingCardinal(request.cardinal)
                every { accountRepository.findByCardinal(request.cardinal) } returns account
                every { receiptRepository.findById(receiptId) } returns Optional.of(receipt)
                every { fileReader.findAll(FileOwnerType.RECEIPT, receiptId, null) } returns oldFiles
                every { fileMapper.toFileList(request.files, FileOwnerType.RECEIPT, receiptId) } returns newFiles

                useCase.update(receiptId, request)

                verify(exactly = 1) { fileRepository.deleteAll(oldFiles) }
                verify(exactly = 1) { fileRepository.saveAll(newFiles) }
            }

            it("다른 기수의 장부에 속한 영수증을 수정하면 ReceiptAccountMismatchException을 던진다") {
                val receiptId = 20L
                val accountA = AccountTestFixture.createAccount(id = 1L, cardinal = 40)
                val accountB = AccountTestFixture.createAccount(id = 2L, cardinal = 41)
                val receipt = ReceiptTestFixture.createReceipt(id = receiptId, amount = 1_000, account = accountB)
                val request = ReceiptUpdateRequest("desc", "source", 2_000, LocalDate.of(2026, 1, 1), 40, null)

                stubExistingCardinal(request.cardinal)
                every { accountRepository.findByCardinal(request.cardinal) } returns accountA
                every { receiptRepository.findById(receiptId) } returns Optional.of(receipt)

                shouldThrow<ReceiptAccountMismatchException> { useCase.update(receiptId, request) }
            }

            it("빈 리스트로 업데이트 시 기존 파일을 모두 삭제한다") {
                val receiptId = 11L
                val account = AccountTestFixture.createAccount(cardinal = 40)
                val receipt = ReceiptTestFixture.createReceipt(id = receiptId, amount = 1_000, account = account)
                account.spend(Money.of(receipt.amount))
                val request =
                    ReceiptUpdateRequest(
                        "desc",
                        "source",
                        2_000,
                        LocalDate.of(2026, 1, 1),
                        40,
                        emptyList(),
                    )
                val oldFiles = listOf(mockk<File>())

                stubExistingCardinal(request.cardinal)
                every { accountRepository.findByCardinal(request.cardinal) } returns account
                every { receiptRepository.findById(receiptId) } returns Optional.of(receipt)
                every { fileReader.findAll(FileOwnerType.RECEIPT, receiptId, null) } returns oldFiles
                every { fileMapper.toFileList(emptyList(), FileOwnerType.RECEIPT, receiptId) } returns emptyList()

                useCase.update(receiptId, request)

                verify(exactly = 1) { fileRepository.deleteAll(oldFiles) }
                verify(exactly = 1) { fileRepository.saveAll(emptyList()) }
            }
        }

        describe("delete") {
            it("관련 파일 삭제 후 cancelSpend가 호출되고 영수증이 삭제된다") {
                val receiptId = 5L
                val account = AccountTestFixture.createAccount(currentAmount = 100_000)
                val receipt = ReceiptTestFixture.createReceipt(id = receiptId, amount = 10_000, account = account)
                account.spend(Money.of(receipt.amount))
                val files = listOf(mockk<File>())

                every { receiptRepository.findById(receiptId) } returns Optional.of(receipt)
                every { fileReader.findAll(FileOwnerType.RECEIPT, receiptId, null) } returns files

                useCase.delete(receiptId)

                verify(exactly = 1) { fileRepository.deleteAll(files) }
                verify(exactly = 1) { receiptRepository.delete(receipt) }
            }
        }
    })
