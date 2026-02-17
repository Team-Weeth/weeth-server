package com.weeth.domain.account.application.usecase

import com.weeth.domain.account.application.dto.ReceiptDTO
import com.weeth.domain.account.application.mapper.ReceiptMapper
import com.weeth.domain.account.domain.entity.Account
import com.weeth.domain.account.domain.entity.Receipt
import com.weeth.domain.account.domain.service.AccountGetService
import com.weeth.domain.account.domain.service.ReceiptDeleteService
import com.weeth.domain.account.domain.service.ReceiptGetService
import com.weeth.domain.account.domain.service.ReceiptSaveService
import com.weeth.domain.account.domain.service.ReceiptUpdateService
import com.weeth.domain.file.application.dto.request.FileSaveRequest
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.entity.FileOwnerType
import com.weeth.domain.file.domain.repository.FileReader
import com.weeth.domain.file.domain.repository.FileRepository
import com.weeth.domain.user.domain.service.CardinalGetService
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate

class ReceiptUseCaseImplTest :
    DescribeSpec({
        val receiptGetService = mockk<ReceiptGetService>()
        val receiptDeleteService = mockk<ReceiptDeleteService>()
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

        describe("update") {
            it("업데이트 파일이 있으면 기존 파일을 삭제 후 새 파일을 저장한다") {
                val receiptId = 10L
                val account =
                    Account
                        .builder()
                        .id(1L)
                        .totalAmount(10000)
                        .currentAmount(10000)
                        .cardinal(40)
                        .receipts(mutableListOf())
                        .build()
                val receipt =
                    Receipt
                        .builder()
                        .id(receiptId)
                        .amount(1000)
                        .account(account)
                        .build()

                val dto =
                    ReceiptDTO.Update(
                        "desc",
                        "source",
                        2000,
                        LocalDate.of(2026, 1, 1),
                        40,
                        listOf(FileSaveRequest("new.png", "TEMP/2026-02/new.png", 100L, "image/png")),
                    )

                val oldFiles = listOf(mockk<File>())
                val newFiles = listOf(mockk<File>())

                every { accountGetService.find(dto.cardinal()) } returns account
                every { receiptGetService.find(receiptId) } returns receipt
                every { fileReader.findAll(FileOwnerType.RECEIPT, receiptId, null) } returns oldFiles
                every { fileMapper.toFileList(dto.files(), FileOwnerType.RECEIPT, receiptId) } returns newFiles

                useCase.update(receiptId, dto)

                verify(exactly = 1) { fileRepository.deleteAll(oldFiles) }
                verify(exactly = 1) { fileRepository.saveAll(newFiles) }
                verify(exactly = 1) { receiptUpdateService.update(receipt, dto) }
            }
        }
    })
