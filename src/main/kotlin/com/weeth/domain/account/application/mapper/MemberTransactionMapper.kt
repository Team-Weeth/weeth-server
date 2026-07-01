package com.weeth.domain.account.application.mapper

import com.weeth.domain.account.application.dto.response.MemberTransactionDetailResponse
import com.weeth.domain.account.application.dto.response.MemberTransactionResponse
import com.weeth.domain.account.domain.entity.AccountTransaction
import com.weeth.domain.account.domain.enums.AccountTransactionType
import com.weeth.domain.file.application.dto.response.FileResponse
import org.springframework.stereotype.Component

@Component
class MemberTransactionMapper {
    fun toResponse(
        transaction: AccountTransaction,
        hasReceipt: Boolean,
    ): MemberTransactionResponse =
        MemberTransactionResponse(
            transactionId = transaction.id,
            type = transaction.type,
            direction = transaction.direction,
            title = transaction.title,
            source = transaction.memberVisibleSource(),
            amount = transaction.amount,
            transactedAt = transaction.transactedAt,
            hasReceipt = hasReceipt,
        )

    fun toDetailResponse(
        transaction: AccountTransaction,
        receipts: List<FileResponse>,
    ): MemberTransactionDetailResponse =
        MemberTransactionDetailResponse(
            transactionId = transaction.id,
            type = transaction.type,
            direction = transaction.direction,
            title = transaction.title,
            source = transaction.memberVisibleSource(),
            amount = transaction.amount,
            transactedAt = transaction.transactedAt,
            category = transaction.category,
            registeredByName = transaction.registeredByName ?: DEFAULT_REGISTERED_BY_NAME,
            memo = transaction.memo,
            hasReceipt = receipts.isNotEmpty(),
            receipts = receipts,
        )

    private fun AccountTransaction.memberVisibleSource(): String? =
        if (type == AccountTransactionType.REFUND) REFUND_SOURCE else source

    companion object {
        private const val REFUND_SOURCE = "환불"
        private const val DEFAULT_REGISTERED_BY_NAME = "운영진"
    }
}
