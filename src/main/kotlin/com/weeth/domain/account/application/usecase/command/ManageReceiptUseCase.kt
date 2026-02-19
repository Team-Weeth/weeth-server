package com.weeth.domain.account.application.usecase.command

import com.weeth.domain.account.application.dto.request.ReceiptSaveRequest
import com.weeth.domain.account.application.dto.request.ReceiptUpdateRequest
import com.weeth.domain.account.application.exception.AccountNotFoundException
import com.weeth.domain.account.application.exception.ReceiptNotFoundException
import com.weeth.domain.account.domain.entity.Receipt
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.account.domain.repository.ReceiptRepository
import com.weeth.domain.account.domain.vo.Money
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.entity.FileOwnerType
import com.weeth.domain.file.domain.repository.FileReader
import com.weeth.domain.file.domain.repository.FileRepository
import com.weeth.domain.user.domain.service.CardinalGetService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ManageReceiptUseCase(
    private val receiptRepository: ReceiptRepository,
    private val accountRepository: AccountRepository,
    private val fileReader: FileReader,
    private val fileRepository: FileRepository,
    private val cardinalGetService: CardinalGetService,
    private val fileMapper: FileMapper,
) {
    @Transactional
    fun save(request: ReceiptSaveRequest) {
        cardinalGetService.findByAdminSide(request.cardinal)
        val account = accountRepository.findByCardinal(request.cardinal) ?: throw AccountNotFoundException()
        val receipt =
            receiptRepository.save(
                Receipt.create(request.description, request.source, request.amount, request.date, account),
            )
        account.spend(Money.of(request.amount))
        fileRepository.saveAll(fileMapper.toFileList(request.files, FileOwnerType.RECEIPT, receipt.id))
    }

    @Transactional
    fun update(
        receiptId: Long,
        request: ReceiptUpdateRequest,
    ) {
        cardinalGetService.findByAdminSide(request.cardinal)
        val account = accountRepository.findByCardinal(request.cardinal) ?: throw AccountNotFoundException()
        val receipt = receiptRepository.findByIdOrNull(receiptId) ?: throw ReceiptNotFoundException()
        account.adjustSpend(Money.of(receipt.amount), Money.of(request.amount))
        if (request.files != null) {
            fileRepository.deleteAll(fileReader.findAll(FileOwnerType.RECEIPT, receiptId, null))
            fileRepository.saveAll(fileMapper.toFileList(request.files, FileOwnerType.RECEIPT, receiptId))
        }
        receipt.update(request.description, request.source, request.amount, request.date)
    }

    @Transactional
    fun delete(receiptId: Long) {
        val receipt = receiptRepository.findByIdOrNull(receiptId) ?: throw ReceiptNotFoundException()
        receipt.account.cancelSpend(Money.of(receipt.amount))
        fileRepository.deleteAll(fileReader.findAll(FileOwnerType.RECEIPT, receiptId, null))
        receiptRepository.delete(receipt)
    }
}
