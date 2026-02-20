package com.weeth.domain.account.application.mapper

import com.weeth.domain.account.application.dto.response.AccountResponse
import com.weeth.domain.account.application.dto.response.ReceiptResponse
import com.weeth.domain.account.domain.entity.Account
import org.springframework.stereotype.Component

@Component
class AccountMapper {
    fun toResponse(
        account: Account,
        receipts: List<ReceiptResponse>,
    ): AccountResponse =
        AccountResponse(
            accountId = account.id,
            description = account.description,
            totalAmount = account.totalAmount,
            currentAmount = account.currentAmount,
            time = account.modifiedAt,
            cardinal = account.cardinal,
            receipts = receipts,
        )
}
