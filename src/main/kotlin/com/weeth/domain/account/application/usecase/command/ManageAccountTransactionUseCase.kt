package com.weeth.domain.account.application.usecase.command

import com.weeth.domain.account.application.dto.request.SaveAccountTransactionRequest
import com.weeth.domain.account.application.dto.request.UpdateAccountTransactionRequest
import com.weeth.domain.account.application.dto.response.AccountTransactionResponse
import com.weeth.domain.account.application.exception.AccountNotActiveException
import com.weeth.domain.account.application.exception.AccountNotFoundException
import com.weeth.domain.account.application.exception.AccountTransactionNotFoundException
import com.weeth.domain.account.application.exception.AccountTransactionTypeNotAllowedException
import com.weeth.domain.account.application.mapper.AccountTransactionMapper
import com.weeth.domain.account.application.usecase.validateOwnedBy
import com.weeth.domain.account.domain.entity.Account
import com.weeth.domain.account.domain.entity.AccountTransaction
import com.weeth.domain.account.domain.enums.AccountStatus
import com.weeth.domain.account.domain.enums.AccountTransactionType
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.account.domain.repository.AccountTransactionRepository
import com.weeth.domain.account.domain.vo.Money
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.file.application.dto.request.FileSaveRequest
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.repository.FileReader
import com.weeth.domain.file.domain.repository.FileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ManageAccountTransactionUseCase(
    private val accountRepository: AccountRepository,
    private val transactionRepository: AccountTransactionRepository,
    private val clubPermissionPolicy: ClubPermissionPolicy,
    private val fileRepository: FileRepository,
    private val fileReader: FileReader,
    private val fileMapper: FileMapper,
    private val accountTransactionMapper: AccountTransactionMapper,
) {
    @Transactional
    fun save(
        clubId: Long,
        accountId: Long,
        request: SaveAccountTransactionRequest,
        userId: Long,
    ): AccountTransactionResponse {
        val admin = clubPermissionPolicy.requireAdmin(clubId, userId)
        val account = getActiveAccountWithLock(clubId, accountId)
        requireManualType(request.type)

        val transaction = accountTransactionMapper.toEntity(account, request, registeredByName = admin.user.name)

        val savedTransaction = transactionRepository.save(transaction)
        account.applyTransaction(savedTransaction)
        account.markModifiedBy(userId)
        val receipts = saveTransactionReceipts(savedTransaction, request.files)

        return accountTransactionMapper.toResponse(savedTransaction, receipts.map(fileMapper::toFileResponse))
    }

    @Transactional
    fun update(
        clubId: Long,
        accountId: Long,
        transactionId: Long,
        request: UpdateAccountTransactionRequest,
        userId: Long,
    ): AccountTransactionResponse {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        val account = getActiveAccountWithLock(clubId, accountId)
        val transaction = getManualTransaction(transactionId, account)
        // 유형 변경 시에만 직접 생성 유형(INCOME/EXPENSE)인지 검증한다. null이면 기존 유형을 유지한다.
        request.type?.let { requireManualType(it) }

        // 반영된 거래는 되돌린 뒤 수정해야 잔액이 새 금액 기준으로 재계산된다.
        account.revertTransaction(transaction)
        transaction.update(
            type = request.type,
            title = request.title,
            source = request.source,
            amount = request.amount?.let { Money.of(it) },
            transactedAt = request.transactedAt?.atStartOfDay(),
            memo = request.memo,
        )

        account.applyTransaction(transaction)
        account.markModifiedBy(userId)
        val receipts = replaceTransactionReceipts(transaction, request.files)

        return accountTransactionMapper.toResponse(transaction, receipts.map(fileMapper::toFileResponse))
    }

    @Transactional
    fun delete(
        clubId: Long,
        accountId: Long,
        transactionId: Long,
        userId: Long,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        val account = getActiveAccountWithLock(clubId, accountId)
        val transaction = getManualTransaction(transactionId, account)

        account.revertTransaction(transaction)
        transaction.softDelete()
        account.markModifiedBy(userId)
    }

    private fun getActiveAccountWithLock(
        clubId: Long,
        accountId: Long,
    ): Account {
        val account = accountRepository.findByIdWithLock(accountId) ?: throw AccountNotFoundException()
        account.validateOwnedBy(clubId)

        if (account.status != AccountStatus.ACTIVE) throw AccountNotActiveException()

        return account
    }

    private fun getManualTransaction(
        transactionId: Long,
        account: Account,
    ): AccountTransaction {
        val transaction =
            transactionRepository.findByIdAndDeletedAtIsNull(transactionId)
                ?: throw AccountTransactionNotFoundException()

        if (transaction.account.id != account.id) throw AccountTransactionNotFoundException()

        // 시스템 생성 거래(납부/이월/환불)는 거래내역 화면에서 직접 수정·삭제할 수 없다.
        if (transaction.type !in MANUAL_TYPES) throw AccountTransactionTypeNotAllowedException()

        return transaction
    }

    private fun requireManualType(type: AccountTransactionType) {
        if (type !in MANUAL_TYPES) throw AccountTransactionTypeNotAllowedException()
    }

    private fun replaceTransactionReceipts(
        transaction: AccountTransaction,
        files: List<FileSaveRequest>?,
    ): List<File> {
        if (files == null) {
            return findTransactionReceipts(transaction.id)
        }

        deleteTransactionReceipts(transaction.id)
        return saveTransactionReceipts(transaction, files)
    }

    private fun saveTransactionReceipts(
        transaction: AccountTransaction,
        files: List<FileSaveRequest>?,
    ): List<File> {
        val mappedFiles = fileMapper.toFileList(files, FileOwnerType.ACCOUNT_TRANSACTION, transaction.id)
        if (mappedFiles.isEmpty()) {
            return emptyList()
        }
        return fileRepository.saveAll(mappedFiles).toList()
    }

    private fun deleteTransactionReceipts(transactionId: Long) {
        val receipts = findTransactionReceipts(transactionId)
        if (receipts.isNotEmpty()) {
            fileRepository.deleteAll(receipts)
            fileRepository.flush()
        }
    }

    private fun findTransactionReceipts(transactionId: Long): List<File> =
        fileReader.findAll(FileOwnerType.ACCOUNT_TRANSACTION, transactionId)

    companion object {
        private val MANUAL_TYPES = setOf(AccountTransactionType.INCOME, AccountTransactionType.EXPENSE)
    }
}
