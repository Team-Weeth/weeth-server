package com.weeth.domain.account.application.mapper

import com.weeth.domain.account.application.dto.request.SaveAccountTransactionRequest
import com.weeth.domain.account.application.dto.response.AccountTransactionResponse
import com.weeth.domain.account.domain.entity.Account
import com.weeth.domain.account.domain.entity.AccountTransaction
import com.weeth.domain.account.domain.vo.Money
import com.weeth.domain.file.application.dto.response.FileResponse
import org.springframework.stereotype.Component

@Component
class AccountTransactionMapper {
    fun toEntity(
        account: Account,
        request: SaveAccountTransactionRequest,
        registeredByName: String? = null,
    ): AccountTransaction =
        AccountTransaction.create(
            account = account,
            type = request.type,
            title = request.title,
            source = request.source,
            amount = Money.of(request.amount),
            transactedAt = request.transactedAt,
            memo = request.memo,
            registeredByName = registeredByName,
        )

    fun toResponse(
        transaction: AccountTransaction,
        receipts: List<FileResponse> = emptyList(),
        hasReceipt: Boolean = receipts.isNotEmpty(),
    ): AccountTransactionResponse =
        AccountTransactionResponse(
            transactionId = transaction.id,
            type = transaction.type,
            direction = transaction.direction,
            title = transaction.title,
            source = transaction.source,
            amount = transaction.amount,
            transactedAt = transaction.transactedAt,
            memo = transaction.memo,
            hasReceipt = hasReceipt,
            receipts = receipts,
        )
}
