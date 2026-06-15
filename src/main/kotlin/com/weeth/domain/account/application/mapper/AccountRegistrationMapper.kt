package com.weeth.domain.account.application.mapper

import com.weeth.domain.account.application.dto.response.AccountRegistrationStatusResponse
import com.weeth.domain.account.application.dto.response.BankAccountResponse
import com.weeth.domain.account.domain.entity.Account
import com.weeth.domain.account.domain.enums.AccountRegistrationStep
import org.springframework.stereotype.Component

@Component
class AccountRegistrationMapper {
    fun toResponse(
        account: Account,
        paymentTargetCount: Int?,
        excludedTargetCount: Int?,
        previousAccount: Account?,
    ): AccountRegistrationStatusResponse {
        val step = account.registrationStep

        return AccountRegistrationStatusResponse(
            accountId = account.id,
            registrationStep = step,
            basic =
                account.name?.let { name ->
                    AccountRegistrationStatusResponse.BasicInfoResponse(
                        name = name,
                        duesAmount = account.duesAmount,
                        description = account.description,
                    )
                },
            carryOver =
                if (step.isAtLeast(AccountRegistrationStep.BANK_ACCOUNT)) {
                    AccountRegistrationStatusResponse.CarryOverResponse(
                        enabled = account.carryOverEnabled,
                        amount = account.carryOverAmount,
                        memo = account.carryOverMemo,
                    )
                } else {
                    null
                },
            paymentTargets =
                paymentTargetCount?.let {
                    AccountRegistrationStatusResponse.PaymentTargetsResponse(
                        targetCount = it,
                        excludedCount = excludedTargetCount ?: 0,
                    )
                },
            bankAccount =
                if (step.isAtLeast(AccountRegistrationStep.REVIEW)) {
                    AccountRegistrationStatusResponse.BankAccountRegistrationResponse(
                        bankAccountVisible = account.bankAccountVisible,
                        bankAccount =
                            account.bankAccount?.let { ba ->
                                BankAccountResponse(ba.bankName, ba.accountNumber, ba.holder, ba.guide)
                            },
                    )
                } else {
                    null
                },
            previousAccountBalance =
                previousAccount?.let {
                    AccountRegistrationStatusResponse.PreviousAccountBalanceResponse(
                        cardinalNumber = it.cardinal,
                        balance = it.currentBalance,
                    )
                },
        )
    }
}
