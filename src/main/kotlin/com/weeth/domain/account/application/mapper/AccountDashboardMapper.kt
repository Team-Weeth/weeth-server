package com.weeth.domain.account.application.mapper

import com.weeth.domain.account.application.dto.response.AccountDashboardResponse
import com.weeth.domain.account.application.dto.response.BankAccountResponse
import com.weeth.domain.account.application.dto.response.MonthlyBalanceResponse
import com.weeth.domain.account.application.dto.response.PaymentSummaryResponse
import com.weeth.domain.account.domain.entity.Account
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import org.springframework.stereotype.Component
import java.time.YearMonth

@Component
class AccountDashboardMapper(
    private val fileAccessUrlPort: FileAccessUrlPort,
) {
    fun toResponse(
        account: Account,
        memberVisible: Boolean,
        totalAmount: Int,
        paidCount: Int,
        totalTargetCount: Int,
        period: Pair<YearMonth, YearMonth>,
        monthlyBalances: List<MonthlyBalanceResponse>,
        modifier: ClubMember?,
    ): AccountDashboardResponse =
        AccountDashboardResponse(
            accountId = account.id,
            summary =
                AccountDashboardResponse.SummaryResponse(
                    // 총 회비(목표액)는 레거시 account.totalAmount 가 아니라 납부 대상 dueAmount 합으로 live 계산한다.
                    totalAmount = totalAmount,
                    currentBalance = account.currentBalance,
                ),
            paymentSummary =
                PaymentSummaryResponse(
                    paidCount = paidCount,
                    totalTargetCount = totalTargetCount,
                ),
            memberVisible = memberVisible,
            bankAccountPublic = account.bankAccountVisible,
            bankAccount = BankAccountResponse.from(account.bankAccount),
            lastModified =
                AccountDashboardResponse.LastModifiedResponse(
                    modifiedAt = account.modifiedAt,
                    modifiedBy =
                        modifier?.let {
                            AccountDashboardResponse.ModifierResponse(
                                userId = it.user.id,
                                name = it.user.name,
                                profileImageUrl = it.profileImageStorageKey?.let(fileAccessUrlPort::resolve),
                            )
                        },
                ),
            period =
                AccountDashboardResponse.PeriodResponse(
                    startYearMonth = period.first.toString(),
                    endYearMonth = period.second.toString(),
                ),
            monthlyBalances = monthlyBalances,
        )
}
