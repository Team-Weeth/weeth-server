package com.weeth.domain.account.application.usecase.query

import com.weeth.domain.account.application.dto.response.AccountDashboardResponse
import com.weeth.domain.account.application.dto.response.MonthlyBalanceResponse
import com.weeth.domain.account.application.exception.AccountNotActiveException
import com.weeth.domain.account.application.exception.AccountNotFoundException
import com.weeth.domain.account.application.mapper.AccountDashboardMapper
import com.weeth.domain.account.application.usecase.validateOwnedBy
import com.weeth.domain.account.domain.entity.Account
import com.weeth.domain.account.domain.entity.AccountTransaction
import com.weeth.domain.account.domain.enums.AccountPaymentStatus
import com.weeth.domain.account.domain.enums.AccountStatus
import com.weeth.domain.account.domain.enums.AccountTargetStatus
import com.weeth.domain.account.domain.enums.AccountTransactionDirection
import com.weeth.domain.account.domain.repository.AccountPaymentTargetRepository
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.account.domain.repository.AccountSettingRepository
import com.weeth.domain.account.domain.repository.AccountTransactionRepository
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.YearMonth

@Service
@Transactional(readOnly = true)
class GetAccountDashboardQueryService(
    private val accountRepository: AccountRepository,
    private val accountSettingRepository: AccountSettingRepository,
    private val transactionRepository: AccountTransactionRepository,
    private val paymentTargetRepository: AccountPaymentTargetRepository,
    private val clubMemberReader: ClubMemberReader,
    private val clubPermissionPolicy: ClubPermissionPolicy,
    private val accountDashboardMapper: AccountDashboardMapper,
    private val clock: Clock,
) {
    fun getDashboard(
        clubId: Long,
        cardinal: Int,
        userId: Long,
    ): AccountDashboardResponse {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        val account =
            accountRepository.findByClubIdAndCardinal(clubId, cardinal)
                ?: throw AccountNotFoundException()
        account.validateOwnedBy(clubId)
        if (!account.isActive) throw AccountNotActiveException()
        val accountId = account.id

        // 총 회비(목표액)는 레거시 account.totalAmount 가 아니라 납부 대상 dueAmount 합으로 live 계산한다.
        val totalAmount = paymentTargetRepository.sumDueAmountByAccountId(accountId).toInt()
        val totalTargetCount =
            paymentTargetRepository.countByAccountIdAndTargetStatus(accountId, AccountTargetStatus.TARGETED)
        val paidCount =
            paymentTargetRepository.countByAccountIdAndTargetStatusAndPaymentStatus(
                accountId,
                AccountTargetStatus.TARGETED,
                AccountPaymentStatus.PAID,
            )

        val transactions = transactionRepository.findByAccountIdAndDeletedAtIsNullOrderByTransactedAtAsc(accountId)
        val period = resolvePeriod(clubId, account, transactions)
        val monthlyBalances = buildMonthlyBalances(period, transactions)

        // 마지막 수정자(userId)를 이 동아리의 부원 프로필(이름/프로필 이미지)로 해석한다.
        // 탈퇴 등으로 더 이상 부원이 아니면 null 로 노출한다.
        val modifier = account.lastModifiedBy?.let { clubMemberReader.findByClubIdAndUserId(clubId, it) }

        return accountDashboardMapper.toResponse(
            account = account,
            memberVisible = accountSettingRepository.isVisibleToMembers(clubId),
            totalAmount = totalAmount,
            paidCount = paidCount.toInt(),
            totalTargetCount = totalTargetCount.toInt(),
            period = period,
            monthlyBalances = monthlyBalances,
            modifier = modifier,
        )
    }

    /**
     * 기수별 회비 생애주기에 맞춘 동적 월 범위(§3.1).
     * - 시작월 = 가장 이른 거래의 월, 거래가 없으면 장부 생성월
     * - 종료월 = 다음 기수 ACTIVE 장부가 있으면 그 장부 시작월의 직전 월, 없으면 현재 월
     */
    private fun resolvePeriod(
        clubId: Long,
        account: Account,
        transactions: List<AccountTransaction>,
    ): Pair<YearMonth, YearMonth> {
        val startYearMonth =
            transactions.firstOrNull()?.let { YearMonth.from(it.transactedAt) }
                ?: YearMonth.from(account.createdAt)

        val nextAccount =
            accountRepository.findTopByClubIdAndCardinalGreaterThanAndStatusOrderByCardinalAsc(
                clubId,
                account.cardinal,
                AccountStatus.ACTIVE,
            )
        val endYearMonth =
            nextAccount
                ?.let { nextAccountStartMonth(it).minusMonths(1) }
                ?: YearMonth.now(clock)

        // 데이터 정합성 방어: 종료월이 시작월보다 앞서지 않도록 보정한다.
        return startYearMonth to maxOf(startYearMonth, endYearMonth)
    }

    private fun nextAccountStartMonth(nextAccount: Account): YearMonth =
        transactionRepository
            .findTopByAccountIdAndDeletedAtIsNullOrderByTransactedAtAsc(nextAccount.id)
            ?.let { YearMonth.from(it.transactedAt) }
            ?: YearMonth.from(nextAccount.createdAt)

    private fun buildMonthlyBalances(
        period: Pair<YearMonth, YearMonth>,
        transactions: List<AccountTransaction>,
    ): List<MonthlyBalanceResponse> {
        val (rawStart, end) = period
        // 데이터 이상치(과거로 잘못 입력된 거래일)로 범위가 폭주하지 않도록 시작월에 하한을 둔다.
        val start = maxOf(rawStart, end.minusMonths((MAX_MONTHLY_BALANCE_MONTHS - 1).toLong()))
        val byMonth = transactions.groupBy { YearMonth.from(it.transactedAt) }

        // 시작월을 잘라낸 만큼 그 이전 거래의 순증감으로 누적 잔액을 시드해, 마지막 endingBalance 가 currentBalance 와 일치하도록 한다.
        val beforeWindow = transactions.filter { YearMonth.from(it.transactedAt).isBefore(start) }
        val seedBalance =
            beforeWindow.filter { it.direction == AccountTransactionDirection.INCOME }.sumOf { it.amount } -
                beforeWindow.filter { it.direction == AccountTransactionDirection.EXPENSE }.sumOf { it.amount }

        val result = mutableListOf<MonthlyBalanceResponse>()
        var runningBalance = seedBalance
        var month = start
        while (!month.isAfter(end)) {
            val monthly = byMonth[month].orEmpty()
            val income = monthly.filter { it.direction == AccountTransactionDirection.INCOME }.sumOf { it.amount }
            val expense = monthly.filter { it.direction == AccountTransactionDirection.EXPENSE }.sumOf { it.amount }
            runningBalance += income - expense
            result.add(
                MonthlyBalanceResponse(
                    yearMonth = month.toString(),
                    income = income,
                    expense = expense,
                    endingBalance = runningBalance,
                ),
            )
            month = month.plusMonths(1)
        }
        return result
    }

    companion object {
        // 월별 잔액 추이의 최대 표시 개월 수(데이터 이상치 방어 상한). 한 기수 회비 생애주기를 충분히 덮는다.
        private const val MAX_MONTHLY_BALANCE_MONTHS = 60
    }
}
