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
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ManageAccountTransactionUseCase(
    private val accountRepository: AccountRepository,
    private val transactionRepository: AccountTransactionRepository,
    private val clubPermissionPolicy: ClubPermissionPolicy,
    private val accountTransactionMapper: AccountTransactionMapper,
) {
    @Transactional
    fun save(
        clubId: Long,
        accountId: Long,
        request: SaveAccountTransactionRequest,
        userId: Long,
    ): AccountTransactionResponse {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        val account = getActiveAccountWithLock(clubId, accountId)
        requireManualType(request.type)

        val transaction = accountTransactionMapper.toEntity(account, request)

        transactionRepository.save(transaction)
        account.applyTransaction(transaction)
        account.markModifiedBy(userId)

        return accountTransactionMapper.toResponse(transaction)
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

        return accountTransactionMapper.toResponse(transaction)
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

    companion object {
        private val MANUAL_TYPES = setOf(AccountTransactionType.INCOME, AccountTransactionType.EXPENSE)
    }
}
