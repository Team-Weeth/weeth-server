package com.weeth.domain.account.application.usecase.query

import com.weeth.domain.account.application.dto.request.AccountTransactionFilter
import com.weeth.domain.account.application.dto.request.AccountTransactionSort
import com.weeth.domain.account.application.dto.response.MemberAccountTransactionsResponse
import com.weeth.domain.account.application.dto.response.MemberTransactionDetailResponse
import com.weeth.domain.account.application.exception.AccountTransactionNotFoundException
import com.weeth.domain.account.application.mapper.MemberTransactionMapper
import com.weeth.domain.account.application.usecase.MemberAccountAccessResolver
import com.weeth.domain.account.domain.entity.AccountTransaction
import com.weeth.domain.account.domain.enums.AccountTransactionType
import com.weeth.domain.account.domain.repository.AccountTransactionRepository
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.repository.FileReader
import com.weeth.global.common.response.SliceResponse
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.SliceImpl
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetMyAccountTransactionQueryService(
    private val transactionRepository: AccountTransactionRepository,
    private val memberAccountAccessResolver: MemberAccountAccessResolver,
    private val fileReader: FileReader,
    private val fileMapper: FileMapper,
    private val memberTransactionMapper: MemberTransactionMapper,
) {
    fun findTransactions(
        clubId: Long,
        cardinal: Int,
        filter: AccountTransactionFilter,
        sort: AccountTransactionSort,
        page: Int,
        size: Int,
        userId: Long,
    ): MemberAccountTransactionsResponse {
        val (account, member) = memberAccountAccessResolver.resolve(clubId, cardinal, userId)
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, MAX_PAGE_SIZE), sort.toSort())

        val transactions =
            if (filter == AccountTransactionFilter.DUES) {
                SliceImpl(emptyList(), pageable, false)
            } else {
                val visibility = filter.toVisibility()
                transactionRepository.findMemberVisibleTransactions(
                    accountId = account.id,
                    clubMemberId = member.id,
                    publicTypes = visibility.publicTypes,
                    includeRefund = visibility.includeRefund,
                    pageable = pageable,
                )
            }

        val transactionIds = transactions.content.map { it.id }
        val transactionIdsWithReceipt =
            fileReader
                .findAll(FileOwnerType.ACCOUNT_TRANSACTION, transactionIds)
                .map { it.ownerId }
                .toSet()

        // 카운트·회비 집계는 페이지마다 변하지 않는 헤더 데이터이므로 첫 페이지에서만 계산한다.
        // 무한 스크롤 후속 페이지에서는 count 쿼리 3회 + 합계 쿼리 1회를 생략한다.
        val (counts, duesSummary) =
            if (pageable.pageNumber == 0) {
                val duesTotalAmount = transactionRepository.sumNetDuesAmountByAccountId(account.id).toInt()
                countByFilter(account.id, member.id, duesTotalAmount) to
                    MemberAccountTransactionsResponse.DuesSummaryResponse(totalAmount = duesTotalAmount)
            } else {
                null to null
            }

        return MemberAccountTransactionsResponse(
            counts = counts,
            duesSummary = duesSummary,
            transactions =
                SliceResponse.from(
                    transactions.map {
                        memberTransactionMapper.toResponse(
                            transaction = it,
                            hasReceipt = it.id in transactionIdsWithReceipt,
                        )
                    },
                ),
        )
    }

    fun findTransaction(
        clubId: Long,
        cardinal: Int,
        transactionId: Long,
        userId: Long,
    ): MemberTransactionDetailResponse {
        val (account, member) = memberAccountAccessResolver.resolve(clubId, cardinal, userId)
        val transaction =
            transactionRepository.findByIdAndDeletedAtIsNull(transactionId)
                ?: throw AccountTransactionNotFoundException()

        if (transaction.account.id != account.id) throw AccountTransactionNotFoundException()
        if (!transaction.isVisibleToMember(member.id)) throw AccountTransactionNotFoundException()

        val receipts =
            fileReader
                .findAll(FileOwnerType.ACCOUNT_TRANSACTION, transaction.id)
                .map(fileMapper::toFileResponse)

        return memberTransactionMapper.toDetailResponse(transaction, receipts)
    }

    private fun countByFilter(
        accountId: Long,
        clubMemberId: Long,
        duesTotalAmount: Int,
    ): MemberAccountTransactionsResponse.TransactionCountsResponse =
        MemberAccountTransactionsResponse.TransactionCountsResponse(
            all = count(accountId, clubMemberId, ALL_PUBLIC_TYPES, includeRefund = true),
            income = count(accountId, clubMemberId, listOf(AccountTransactionType.INCOME), includeRefund = false),
            expense = count(accountId, clubMemberId, listOf(AccountTransactionType.EXPENSE), includeRefund = true),
            dues = if (duesTotalAmount > 0) 1 else 0,
        )

    private fun count(
        accountId: Long,
        clubMemberId: Long,
        publicTypes: List<AccountTransactionType>,
        includeRefund: Boolean,
    ): Int =
        transactionRepository
            .countMemberVisibleTransactions(
                accountId = accountId,
                clubMemberId = clubMemberId,
                publicTypes = publicTypes,
                includeRefund = includeRefund,
            ).toInt()

    private fun AccountTransactionFilter.toVisibility(): Visibility =
        when (this) {
            AccountTransactionFilter.ALL -> Visibility(ALL_PUBLIC_TYPES, includeRefund = true)
            AccountTransactionFilter.INCOME -> Visibility(listOf(AccountTransactionType.INCOME), includeRefund = false)
            AccountTransactionFilter.EXPENSE -> Visibility(listOf(AccountTransactionType.EXPENSE), includeRefund = true)
            AccountTransactionFilter.DUES -> Visibility(emptyList(), includeRefund = false)
        }

    private fun AccountTransaction.isVisibleToMember(clubMemberId: Long): Boolean =
        when (type) {
            AccountTransactionType.INCOME,
            AccountTransactionType.EXPENSE,
            AccountTransactionType.CARRY_OVER,
            -> true

            AccountTransactionType.REFUND -> paymentTarget?.clubMember?.id == clubMemberId

            AccountTransactionType.DUES -> false
        }

    private data class Visibility(
        val publicTypes: List<AccountTransactionType>,
        val includeRefund: Boolean,
    )

    companion object {
        private const val MAX_PAGE_SIZE = 100

        private val ALL_PUBLIC_TYPES =
            listOf(
                AccountTransactionType.INCOME,
                AccountTransactionType.EXPENSE,
                AccountTransactionType.CARRY_OVER,
            )
    }
}
