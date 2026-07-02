package com.weeth.domain.account.application.usecase.query

import com.weeth.domain.account.application.exception.AccountNotFoundException
import com.weeth.domain.account.application.mapper.AccountDashboardMapper
import com.weeth.domain.account.domain.entity.Account
import com.weeth.domain.account.domain.entity.AccountTransaction
import com.weeth.domain.account.domain.enums.AccountPaymentStatus
import com.weeth.domain.account.domain.enums.AccountStatus
import com.weeth.domain.account.domain.enums.AccountTargetStatus
import com.weeth.domain.account.domain.enums.AccountTransactionType
import com.weeth.domain.account.domain.repository.AccountPaymentTargetRepository
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.account.domain.repository.AccountTransactionRepository
import com.weeth.domain.account.domain.vo.Money
import com.weeth.domain.account.fixture.AccountTestFixture
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class GetAccountDashboardQueryServiceTest :
    DescribeSpec({
        val accountRepository = mockk<AccountRepository>()
        val transactionRepository = mockk<AccountTransactionRepository>()
        val paymentTargetRepository = mockk<AccountPaymentTargetRepository>()
        val clubMemberReader = mockk<ClubMemberReader>()
        val fileAccessUrlPort = mockk<FileAccessUrlPort>()
        val clubPermissionPolicy = mockk<ClubPermissionPolicy>(relaxed = true)
        val clock = Clock.system(ZoneId.of("Asia/Seoul"))
        val queryService =
            GetAccountDashboardQueryService(
                accountRepository,
                transactionRepository,
                paymentTargetRepository,
                clubMemberReader,
                clubPermissionPolicy,
                AccountDashboardMapper(fileAccessUrlPort),
                clock,
            )

        val userId = 10L
        val accountId = 1L
        val cardinal = 40

        fun transaction(
            account: Account,
            type: AccountTransactionType,
            amount: Int,
            date: LocalDate,
        ): AccountTransaction =
            AccountTransaction.create(
                account = account,
                type = type,
                title = "거래",
                source = null,
                amount = Money.of(amount),
                transactedAt = date.atStartOfDay(),
            )

        beforeTest {
            clearMocks(
                accountRepository,
                transactionRepository,
                paymentTargetRepository,
                clubMemberReader,
                fileAccessUrlPort,
            )
            every { paymentTargetRepository.sumDueAmountByAccountId(accountId) } returns 1_390_000L
            every {
                paymentTargetRepository.countByAccountIdAndTargetStatus(accountId, AccountTargetStatus.TARGETED)
            } returns 24
            every {
                paymentTargetRepository.countByAccountIdAndTargetStatusAndPaymentStatus(
                    accountId,
                    AccountTargetStatus.TARGETED,
                    AccountPaymentStatus.PAID,
                )
            } returns 3
        }

        describe("getDashboard") {
            it("다음 기수 장부가 있으면 종료월을 그 장부 시작월의 직전 월로 고정하고 월별 잔액을 누적 집계한다") {
                // currentBalance(저장값) = 거래 순증감(100_000 - 40_000) 으로, 차트 마지막 endingBalance 와 일치하는 정상 상태.
                val account =
                    AccountTestFixture.createAccount(
                        id = accountId,
                        cardinal = cardinal,
                        currentBalance = 60_000,
                    )
                account.markModifiedBy(5L)
                every { accountRepository.findByClubIdAndCardinal(account.club.id, cardinal) } returns account

                val transactions =
                    listOf(
                        transaction(account, AccountTransactionType.CARRY_OVER, 100_000, LocalDate.of(2026, 3, 1)),
                        transaction(account, AccountTransactionType.EXPENSE, 40_000, LocalDate.of(2026, 4, 10)),
                    )
                every {
                    transactionRepository.findByAccountIdAndDeletedAtIsNullOrderByTransactedAtAsc(accountId)
                } returns transactions

                val nextAccount = AccountTestFixture.createAccount(id = 2L, cardinal = 41)
                every {
                    accountRepository.findTopByClubIdAndCardinalGreaterThanAndStatusOrderByCardinalAsc(
                        account.club.id,
                        cardinal,
                        AccountStatus.ACTIVE,
                    )
                } returns nextAccount
                every {
                    transactionRepository.findTopByAccountIdAndDeletedAtIsNullOrderByTransactedAtAsc(2L)
                } returns transaction(nextAccount, AccountTransactionType.CARRY_OVER, 10_000, LocalDate.of(2026, 6, 5))

                val modifierMember =
                    ClubTestFixture
                        .createClubMember(
                            club = account.club,
                            user = UserTestFixture.createActiveUser1(id = 5L),
                        ).also { it.updateProfileImageUrl("profiles/5.png") }
                every { clubMemberReader.findByClubIdAndUserId(account.club.id, 5L) } returns modifierMember
                every { fileAccessUrlPort.resolve("profiles/5.png") } returns "https://cdn.test/profiles/5.png"

                val result = queryService.getDashboard(account.club.id, cardinal, userId)

                result.accountId shouldBe accountId
                result.period.startYearMonth shouldBe "2026-03"
                result.period.endYearMonth shouldBe "2026-05"
                result.monthlyBalances.map { it.yearMonth } shouldBe listOf("2026-03", "2026-04", "2026-05")
                result.monthlyBalances[0].income shouldBe 100_000
                result.monthlyBalances[0].endingBalance shouldBe 100_000
                result.monthlyBalances[1].expense shouldBe 40_000
                result.monthlyBalances[1].endingBalance shouldBe 60_000
                result.monthlyBalances[2].endingBalance shouldBe 60_000
                // 불변식: 상단 요약의 현재 잔액(저장값)은 차트 마지막 달의 누적 잔액(라이브 계산)과 일치한다.
                result.summary.currentBalance shouldBe result.monthlyBalances.last().endingBalance
                result.summary.totalAmount shouldBe 1_390_000
                result.paymentSummary.paidCount shouldBe 3
                result.paymentSummary.totalTargetCount shouldBe 24
                result.lastModified.modifiedBy?.userId shouldBe 5L
                result.lastModified.modifiedBy?.name shouldBe "적순"
                result.lastModified.modifiedBy?.profileImageUrl shouldBe "https://cdn.test/profiles/5.png"
                verify(exactly = 1) { clubMemberReader.findByClubIdAndUserId(account.club.id, 5L) }
            }

            it("다음 기수 장부가 없으면 종료월이 현재 월이고 시작월은 가장 이른 거래의 월이다") {
                val account = AccountTestFixture.createAccount(id = accountId, cardinal = cardinal)
                every { accountRepository.findByClubIdAndCardinal(account.club.id, cardinal) } returns account
                every {
                    transactionRepository.findByAccountIdAndDeletedAtIsNullOrderByTransactedAtAsc(accountId)
                } returns
                    listOf(
                        transaction(account, AccountTransactionType.CARRY_OVER, 50_000, LocalDate.of(2026, 4, 2)),
                    )
                every {
                    accountRepository.findTopByClubIdAndCardinalGreaterThanAndStatusOrderByCardinalAsc(
                        account.club.id,
                        cardinal,
                        AccountStatus.ACTIVE,
                    )
                } returns null

                val result = queryService.getDashboard(account.club.id, cardinal, userId)

                result.period.startYearMonth shouldBe "2026-04"
                result.period.endYearMonth shouldBe YearMonth.now(clock).toString()
                result.lastModified.modifiedBy shouldBe null
                verify(exactly = 0) { clubMemberReader.findByClubIdAndUserId(any(), any()) }
            }

            it("해당 기수의 장부가 없으면 AccountNotFoundException 을 던진다") {
                val clubId = 100L
                every { accountRepository.findByClubIdAndCardinal(clubId, cardinal) } returns null

                shouldThrow<AccountNotFoundException> {
                    queryService.getDashboard(clubId, cardinal, userId)
                }
            }
        }
    })
