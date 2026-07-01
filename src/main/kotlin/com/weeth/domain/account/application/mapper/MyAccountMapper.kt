package com.weeth.domain.account.application.mapper

import com.weeth.domain.account.application.dto.response.AccountCardinalResponse
import com.weeth.domain.account.application.dto.response.BankAccountResponse
import com.weeth.domain.account.application.dto.response.MyAccountResponse
import com.weeth.domain.account.domain.entity.Account
import com.weeth.domain.account.domain.entity.AccountPaymentTarget
import com.weeth.domain.account.domain.enums.AccountTargetStatus
import org.springframework.stereotype.Component

@Component
class MyAccountMapper {
    fun toCardinalResponse(
        account: Account,
        isLatest: Boolean,
    ): AccountCardinalResponse =
        AccountCardinalResponse(
            cardinal = account.cardinal,
            name = account.name,
            isLatest = isLatest,
        )

    fun toResponse(
        account: Account,
        target: AccountPaymentTarget?,
        goalAmount: Int,
    ): MyAccountResponse =
        MyAccountResponse(
            accountId = account.id,
            cardinal = account.cardinal,
            accountName = account.name,
            duesAmount = account.duesAmount,
            myPayment = target.toMyPaymentResponse(),
            bankAccountVisible = account.bankAccountVisible,
            bankAccount = if (account.bankAccountVisible) BankAccountResponse.from(account.bankAccount) else null,
            balance =
                MyAccountResponse.BalanceResponse(
                    currentBalance = account.currentBalance,
                    goalAmount = goalAmount,
                ),
        )

    // 제외(EXCLUDED)·미선택 등 TARGETED 가 아니면 납부 대상이 아니므로 상태를 노출하지 않는다.
    private fun AccountPaymentTarget?.toMyPaymentResponse(): MyAccountResponse.MyPaymentResponse {
        if (this == null || targetStatus != AccountTargetStatus.TARGETED) {
            return MyAccountResponse.MyPaymentResponse(
                targeted = false,
                status = null,
                dueAmount = 0,
                paidAmount = 0,
                paidAt = null,
            )
        }

        return MyAccountResponse.MyPaymentResponse(
            targeted = true,
            status = paymentStatus,
            dueAmount = dueAmount,
            paidAmount = paidAmount,
            paidAt = paidAt,
        )
    }
}
