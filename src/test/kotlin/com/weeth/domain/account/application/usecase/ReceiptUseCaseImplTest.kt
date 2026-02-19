package com.weeth.domain.account.application.usecase

import com.weeth.domain.account.application.dto.request.ReceiptSaveRequest
import com.weeth.domain.account.application.dto.request.ReceiptUpdateRequest
import com.weeth.domain.account.application.mapper.ReceiptMapper
import com.weeth.domain.account.domain.service.AccountGetService
import com.weeth.domain.account.domain.service.ReceiptDeleteService
import com.weeth.domain.account.domain.service.ReceiptGetService
import com.weeth.domain.account.domain.service.ReceiptSaveService
import com.weeth.domain.account.domain.service.ReceiptUpdateService
import com.weeth.domain.account.domain.vo.Money
import com.weeth.domain.account.fixture.AccountTestFixture
import com.weeth.domain.account.fixture.ReceiptTestFixture
import com.weeth.domain.file.application.dto.request.FileSaveRequest
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.entity.FileOwnerType
import com.weeth.domain.file.domain.repository.FileReader
import com.weeth.domain.file.domain.repository.FileRepository
import com.weeth.domain.user.domain.service.CardinalGetService
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate

class ReceiptUseCaseImplTest :
    DescribeSpec({
        val receiptGetService = mockk<ReceiptGetService>()
        val receiptDeleteService = mockk<ReceiptDeleteService>(relaxUnitFun = true)
        val receiptSaveService = mockk<ReceiptSaveService>()
        val receiptUpdateService = mockk<ReceiptUpdateService>(relaxUnitFun = true)
        val accountGetService = mockk<AccountGetService>()
        val fileReader = mockk<FileReader>()
        val fileRepository = mockk<FileRepository>(relaxed = true)
        val cardinalGetService = mockk<CardinalGetService>()
        val receiptMapper = mockk<ReceiptMapper>()
        val fileMapper = mockk<FileMapper>()

        val useCase =
            ReceiptUseCaseImpl(
                receiptGetService,
                receiptDeleteService,
                receiptSaveService,
                receiptUpdateService,
                accountGetService,
                fileReader,
                fileRepository,
                cardinalGetService,
                receiptMapper,
                fileMapper,
            )

        beforeTest {
            clearMocks(
                receiptGetService,
                receiptDeleteService,
                receiptSaveService,
                receiptUpdateService,
                accountGetService,
                fileReader,
                cardinalGetService,
                receiptMapper,
                fileMapper,
            )
        }

        describe("save") {
            context("파일이 있는 경우") {
                it("영수증 저장 후 fileRepository.saveAll이 호출된다") {
                    val account = AccountTestFixture.createAccount(cardinal = 40)
                    val savedReceipt = ReceiptTestFixture.createReceipt(id = 10L, amount = 5_000, account = account)
                    val files = listOf(mockk<File>())
                    val dto =
                        ReceiptSaveRequest(
                            "간식비",
                            "편의점",
                            5_000,
                            LocalDate.of(2024, 9, 1),
                            40,
                            listOf(FileSaveRequest("receipt.png", "TEMP/2024-09/receipt.png", 200L, "image/png")),
                        )

                    every { cardinalGetService.findByAdminSide(40) } returns mockk()
                    every { accountGetService.find(40) } returns account
                    every { receiptSaveService.save(any()) } returns savedReceipt
                    every { fileMapper.toFileList(dto.files, FileOwnerType.RECEIPT, savedReceipt.id) } returns files

                    useCase.save(dto)

                    verify(exactly = 1) { receiptSaveService.save(any()) }
                    verify(exactly = 1) { fileRepository.saveAll(files) }
                }
            }

            context("파일이 없는 경우") {
                it("영수증 저장 후 fileRepository.saveAll은 빈 리스트로 호출된다") {
                    val account = AccountTestFixture.createAccount(cardinal = 40)
                    val savedReceipt = ReceiptTestFixture.createReceipt(id = 11L, amount = 3_000, account = account)
                    val dto = ReceiptSaveRequest("교통비", "지하철", 3_000, LocalDate.of(2024, 9, 2), 40, emptyList())

                    every { cardinalGetService.findByAdminSide(40) } returns mockk()
                    every { accountGetService.find(40) } returns account
                    every { receiptSaveService.save(any()) } returns savedReceipt
                    every { fileMapper.toFileList(emptyList(), FileOwnerType.RECEIPT, savedReceipt.id) } returns emptyList()

                    useCase.save(dto)

                    verify(exactly = 1) { receiptSaveService.save(any()) }
                    verify(exactly = 1) { fileRepository.saveAll(emptyList()) }
                }
            }
        }

        describe("update") {
            it("업데이트 파일이 있으면 기존 파일을 삭제 후 새 파일을 저장한다") {
                val receiptId = 10L
                val account = AccountTestFixture.createAccount(cardinal = 40)
                val receipt = ReceiptTestFixture.createReceipt(id = receiptId, amount = 1_000, account = account)
                account.spend(Money.of(receipt.amount)) // adjustSpend를 위한 사전 spend

                val dto =
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

                every { accountGetService.find(dto.cardinal) } returns account
                every { receiptGetService.find(receiptId) } returns receipt
                every { fileReader.findAll(FileOwnerType.RECEIPT, receiptId, null) } returns oldFiles
                every { fileMapper.toFileList(dto.files, FileOwnerType.RECEIPT, receiptId) } returns newFiles

                useCase.update(receiptId, dto)

                verify(exactly = 1) { fileRepository.deleteAll(oldFiles) }
                verify(exactly = 1) { fileRepository.saveAll(newFiles) }
                verify(exactly = 1) { receiptUpdateService.update(receipt, any()) }
            }
        }

        describe("delete") {
            it("관련 파일 삭제 후 cancelSpend가 호출되고 영수증이 삭제된다") {
                val receiptId = 5L
                val account = AccountTestFixture.createAccount(currentAmount = 100_000)
                val receipt = ReceiptTestFixture.createReceipt(id = receiptId, amount = 10_000, account = account)
                account.spend(Money.of(receipt.amount)) // cancelSpend를 위한 사전 spend
                val files = listOf(mockk<File>())

                every { receiptGetService.find(receiptId) } returns receipt
                every { fileReader.findAll(FileOwnerType.RECEIPT, receiptId, null) } returns files

                useCase.delete(receiptId)

                verify(exactly = 1) { fileRepository.deleteAll(files) }
                verify(exactly = 1) { receiptDeleteService.delete(receipt) }
            }
        }
    })
