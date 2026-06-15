package com.weeth.domain.account.application.usecase.query

import com.weeth.domain.account.application.dto.request.AccountTransactionFilter
import com.weeth.domain.account.application.dto.request.AccountTransactionSort
import com.weeth.domain.account.application.exception.AccountTransactionNotFoundException
import com.weeth.domain.account.application.mapper.AccountTransactionMapper
import com.weeth.domain.account.domain.entity.Account
import com.weeth.domain.account.domain.entity.AccountTransaction
import com.weeth.domain.account.domain.enums.AccountTransactionDirection
import com.weeth.domain.account.domain.enums.AccountTransactionType
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.account.domain.repository.AccountTransactionRepository
import com.weeth.domain.account.domain.vo.Money
import com.weeth.domain.account.fixture.AccountTestFixture
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate
import java.util.Optional

class GetAccountTransactionQueryServiceTest :
    DescribeSpec({
        val accountRepository = mockk<AccountRepository>()
        val transactionRepository = mockk<AccountTransactionRepository>(relaxed = true)
        val clubPermissionPolicy = mockk<ClubPermissionPolicy>(relaxed = true)
        val queryService =
            GetAccountTransactionQueryService(
                accountRepository,
                transactionRepository,
                clubPermissionPolicy,
                AccountTransactionMapper(),
            )

        val userId = 10L
        val accountId = 1L

        fun transaction(
            account: Account,
            id: Long,
            type: AccountTransactionType,
            amount: Int,
        ): AccountTransaction =
            AccountTransaction
                .create(
                    account = account,
                    type = type,
                    title = "거래",
                    source = null,
                    amount = Money.of(amount),
                    transactedAt = LocalDate.of(2026, 7, 20).atStartOfDay(),
                ).also { ReflectionTestUtils.setField(it, "id", id) }

        beforeTest {
            clearMocks(accountRepository, transactionRepository, clubPermissionPolicy)
            every { transactionRepository.countByAccountIdAndDeletedAtIsNull(accountId) } returns 18
            every {
                transactionRepository.countByAccountIdAndTypeAndDeletedAtIsNull(
                    accountId,
                    AccountTransactionType.INCOME,
                )
            } returns 2
            // 회비 탭은 DUES + CARRY_OVER 를 typeIn 으로 집계한다(프로덕션 countByFilter 와 동일 시그니처)
            every {
                transactionRepository.countByAccountIdAndTypeInAndDeletedAtIsNull(
                    accountId,
                    listOf(AccountTransactionType.DUES, AccountTransactionType.CARRY_OVER),
                )
            } returns 3
            every {
                transactionRepository.countByAccountIdAndDirectionAndDeletedAtIsNull(
                    accountId,
                    AccountTransactionDirection.EXPENSE,
                )
            } returns 10
        }

        describe("findTransactions") {
            it("ALL 필터는 삭제 제외 전체 조회를 호출하고 카운트 요약을 채운다") {
                val account = AccountTestFixture.createAccount(id = accountId)
                every { accountRepository.findById(accountId) } returns Optional.of(account)
                val page =
                    PageImpl(listOf(transaction(account, 100L, AccountTransactionType.EXPENSE, 5_000)))
                every { transactionRepository.findByAccountIdAndDeletedAtIsNull(accountId, any()) } returns page

                val result =
                    queryService.findTransactions(
                        account.club.id,
                        accountId,
                        AccountTransactionFilter.ALL,
                        AccountTransactionSort.LATEST,
                        0,
                        20,
                        userId,
                    )

                result.transactions.content
                    .first()
                    .transactionId shouldBe 100L
                result.counts.all shouldBe 18
                result.counts.income shouldBe 2
                result.counts.expense shouldBe 10
                result.counts.dues shouldBe 3
                verify(exactly = 1) { transactionRepository.findByAccountIdAndDeletedAtIsNull(accountId, any()) }
            }

            it("EXPENSE 필터는 지출 방향 조회를 호출한다") {
                val account = AccountTestFixture.createAccount(id = accountId)
                every { accountRepository.findById(accountId) } returns Optional.of(account)
                every {
                    transactionRepository.findByAccountIdAndDirectionAndDeletedAtIsNull(
                        accountId,
                        AccountTransactionDirection.EXPENSE,
                        any(),
                    )
                } returns PageImpl(emptyList(), Pageable.ofSize(20), 0)

                queryService.findTransactions(
                    account.club.id,
                    accountId,
                    AccountTransactionFilter.EXPENSE,
                    AccountTransactionSort.AMOUNT_DESC,
                    0,
                    20,
                    userId,
                )

                verify(exactly = 1) {
                    transactionRepository.findByAccountIdAndDirectionAndDeletedAtIsNull(
                        accountId,
                        AccountTransactionDirection.EXPENSE,
                        any(),
                    )
                }
            }
        }

        describe("findTransaction") {
            it("단건 상세를 반환한다") {
                val account = AccountTestFixture.createAccount(id = accountId)
                every { accountRepository.findById(accountId) } returns Optional.of(account)
                every { transactionRepository.findByIdAndDeletedAtIsNull(100L) } returns
                    transaction(account, 100L, AccountTransactionType.EXPENSE, 5_000)

                val result = queryService.findTransaction(account.club.id, accountId, 100L, userId)

                result.transactionId shouldBe 100L
                result.amount shouldBe 5_000
            }

            it("없는 거래면 NotFound를 던진다") {
                val account = AccountTestFixture.createAccount(id = accountId)
                every { accountRepository.findById(accountId) } returns Optional.of(account)
                every { transactionRepository.findByIdAndDeletedAtIsNull(999L) } returns null

                shouldThrow<AccountTransactionNotFoundException> {
                    queryService.findTransaction(account.club.id, accountId, 999L, userId)
                }
            }

            it("다른 장부의 거래면 NotFound를 던진다") {
                val account = AccountTestFixture.createAccount(id = accountId)
                val otherAccount = AccountTestFixture.createAccount(id = 2L)
                every { accountRepository.findById(accountId) } returns Optional.of(account)
                every { transactionRepository.findByIdAndDeletedAtIsNull(100L) } returns
                    transaction(otherAccount, 100L, AccountTransactionType.EXPENSE, 5_000)

                shouldThrow<AccountTransactionNotFoundException> {
                    queryService.findTransaction(account.club.id, accountId, 100L, userId)
                }
            }
        }
    })
