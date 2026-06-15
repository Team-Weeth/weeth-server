package com.weeth.domain.account.application.usecase.query

import com.weeth.domain.account.application.dto.request.AccountTransactionFilter
import com.weeth.domain.account.application.dto.request.AccountTransactionSort
import com.weeth.domain.account.application.dto.response.AccountTransactionResponse
import com.weeth.domain.account.application.dto.response.AccountTransactionsResponse
import com.weeth.domain.account.application.exception.AccountNotFoundException
import com.weeth.domain.account.application.exception.AccountTransactionNotFoundException
import com.weeth.domain.account.application.mapper.AccountTransactionMapper
import com.weeth.domain.account.application.usecase.validateOwnedBy
import com.weeth.domain.account.domain.entity.AccountTransaction
import com.weeth.domain.account.domain.enums.AccountTransactionDirection
import com.weeth.domain.account.domain.enums.AccountTransactionType
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.account.domain.repository.AccountTransactionRepository
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.global.common.response.PageResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetAccountTransactionQueryService(
    private val accountRepository: AccountRepository,
    private val transactionRepository: AccountTransactionRepository,
    private val clubPermissionPolicy: ClubPermissionPolicy,
    private val accountTransactionMapper: AccountTransactionMapper,
) {
    fun findTransactions(
        clubId: Long,
        accountId: Long,
        filter: AccountTransactionFilter,
        sort: AccountTransactionSort,
        page: Int,
        size: Int,
        userId: Long,
    ): AccountTransactionsResponse {
        requireAdminAccess(clubId, accountId, userId)

        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100), sort.toSort())
        val transactions: Page<AccountTransaction> =
            when (filter) {
                AccountTransactionFilter.ALL -> {
                    transactionRepository.findByAccountIdAndDeletedAtIsNull(accountId, pageable)
                }

                AccountTransactionFilter.INCOME -> {
                    transactionRepository.findByAccountIdAndTypeAndDeletedAtIsNull(
                        accountId,
                        AccountTransactionType.INCOME,
                        pageable,
                    )
                }

                AccountTransactionFilter.DUES -> {
                    transactionRepository.findByAccountIdAndTypeInAndDeletedAtIsNull(
                        accountId,
                        listOf(AccountTransactionType.DUES, AccountTransactionType.CARRY_OVER),
                        pageable,
                    )
                }

                AccountTransactionFilter.EXPENSE -> {
                    transactionRepository.findByAccountIdAndDirectionAndDeletedAtIsNull(
                        accountId,
                        AccountTransactionDirection.EXPENSE,
                        pageable,
                    )
                }
            }

        return AccountTransactionsResponse(
            counts = countByFilter(accountId),
            transactions = PageResponse.from(transactions.map { accountTransactionMapper.toResponse(it) }),
        )
    }

    fun findTransaction(
        clubId: Long,
        accountId: Long,
        transactionId: Long,
        userId: Long,
    ): AccountTransactionResponse {
        requireAdminAccess(clubId, accountId, userId)

        val transaction =
            transactionRepository.findByIdAndDeletedAtIsNull(transactionId)
                ?: throw AccountTransactionNotFoundException()

        if (transaction.account.id != accountId) throw AccountTransactionNotFoundException()

        return accountTransactionMapper.toResponse(transaction)
    }

    private fun requireAdminAccess(
        clubId: Long,
        accountId: Long,
        userId: Long,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)

        val account = accountRepository.findById(accountId).orElseThrow { AccountNotFoundException() }
        account.validateOwnedBy(clubId)
    }

    private fun countByFilter(accountId: Long): AccountTransactionsResponse.TransactionCountsResponse =
        AccountTransactionsResponse.TransactionCountsResponse(
            all = transactionRepository.countByAccountIdAndDeletedAtIsNull(accountId).toInt(),
            income =
                transactionRepository
                    .countByAccountIdAndTypeAndDeletedAtIsNull(accountId, AccountTransactionType.INCOME)
                    .toInt(),
            expense =
                transactionRepository
                    .countByAccountIdAndDirectionAndDeletedAtIsNull(accountId, AccountTransactionDirection.EXPENSE)
                    .toInt(),
            // 이월된 회비(CARRY_OVER)도 회비 탭에 포함
            dues =
                transactionRepository
                    .countByAccountIdAndTypeInAndDeletedAtIsNull(
                        accountId,
                        listOf(AccountTransactionType.DUES, AccountTransactionType.CARRY_OVER),
                    ).toInt(),
        )
}
