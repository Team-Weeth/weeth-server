package com.weeth.domain.account.application.usecase.command

import com.weeth.domain.account.application.dto.request.SaveAccountTransactionRequest
import com.weeth.domain.account.application.dto.request.UpdateAccountTransactionRequest
import com.weeth.domain.account.application.exception.AccountNotActiveException
import com.weeth.domain.account.application.exception.AccountNotFoundException
import com.weeth.domain.account.application.exception.AccountTransactionNotFoundException
import com.weeth.domain.account.application.exception.AccountTransactionTypeNotAllowedException
import com.weeth.domain.account.application.mapper.AccountTransactionMapper
import com.weeth.domain.account.domain.entity.Account
import com.weeth.domain.account.domain.entity.AccountTransaction
import com.weeth.domain.account.domain.enums.AccountStatus
import com.weeth.domain.account.domain.enums.AccountTransactionType
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.account.domain.repository.AccountTransactionRepository
import com.weeth.domain.account.domain.vo.Money
import com.weeth.domain.account.fixture.AccountTestFixture
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate

class ManageAccountTransactionUseCaseTest :
    DescribeSpec({
        val accountRepository = mockk<AccountRepository>()
        val transactionRepository = mockk<AccountTransactionRepository>(relaxed = true)
        val clubPermissionPolicy = mockk<ClubPermissionPolicy>(relaxed = true)
        val useCase =
            ManageAccountTransactionUseCase(
                accountRepository,
                transactionRepository,
                clubPermissionPolicy,
                AccountTransactionMapper(),
            )

        val userId = 10L
        val accountId = 1L
        val transactionId = 100L
        val date = LocalDate.of(2026, 7, 20)

        beforeTest {
            clearMocks(accountRepository, transactionRepository, clubPermissionPolicy)
            every { transactionRepository.save(any()) } answers { firstArg() }
        }

        fun appliedTransaction(
            account: Account,
            type: AccountTransactionType,
            amount: Int,
        ): AccountTransaction =
            AccountTransaction
                .create(
                    account = account,
                    type = type,
                    title = "기존 거래",
                    source = null,
                    amount = Money.of(amount),
                    transactedAt = date.atStartOfDay(),
                ).also { account.applyTransaction(it) }

        describe("save") {
            it("지출 거래를 생성하고 잔액을 차감하며 수정자를 기록한다") {
                val account = AccountTestFixture.createAccount(id = accountId, currentBalance = 100_000)
                every { accountRepository.findByIdWithLock(accountId) } returns account
                val request =
                    SaveAccountTransactionRequest(AccountTransactionType.EXPENSE, 30_000, "스터디 지원금", "인프런", date, null)

                val response = useCase.save(account.club.id, accountId, request, userId)

                response.amount shouldBe 30_000
                response.type shouldBe AccountTransactionType.EXPENSE
                account.currentBalance shouldBe 70_000
                account.lastModifiedBy shouldBe userId
                verify(exactly = 1) { transactionRepository.save(any()) }
            }

            it("시스템 타입(DUES) 생성 요청은 거부한다") {
                val account = AccountTestFixture.createAccount(id = accountId)
                every { accountRepository.findByIdWithLock(accountId) } returns account
                val request = SaveAccountTransactionRequest(AccountTransactionType.DUES, 10_000, "회비", "출처", date, null)

                shouldThrow<AccountTransactionTypeNotAllowedException> {
                    useCase.save(account.club.id, accountId, request, userId)
                }
            }

            it("초안 상태 장부면 운영을 거부한다") {
                val account = AccountTestFixture.createAccount(id = accountId, status = AccountStatus.DRAFT)
                every { accountRepository.findByIdWithLock(accountId) } returns account
                val request =
                    SaveAccountTransactionRequest(AccountTransactionType.EXPENSE, 1_000, "지출", "출처", date, null)

                shouldThrow<AccountNotActiveException> { useCase.save(account.club.id, accountId, request, userId) }
            }

            it("존재하지 않는 장부면 NotFound를 던진다") {
                every { accountRepository.findByIdWithLock(accountId) } returns null
                val request =
                    SaveAccountTransactionRequest(AccountTransactionType.EXPENSE, 1_000, "지출", "출처", date, null)

                shouldThrow<AccountNotFoundException> { useCase.save(1L, accountId, request, userId) }
            }
        }

        describe("update") {
            it("금액 수정 시 잔액을 재계산한다") {
                val account = AccountTestFixture.createAccount(id = accountId, currentBalance = 100_000)
                val transaction = appliedTransaction(account, AccountTransactionType.EXPENSE, 30_000)
                account.currentBalance shouldBe 70_000
                every { accountRepository.findByIdWithLock(accountId) } returns account
                every { transactionRepository.findByIdAndDeletedAtIsNull(transactionId) } returns transaction
                val request =
                    UpdateAccountTransactionRequest(AccountTransactionType.EXPENSE, 50_000, "수정", null, date, null)

                val response = useCase.update(account.club.id, accountId, transactionId, request, userId)

                response.amount shouldBe 50_000
                account.currentBalance shouldBe 50_000
            }

            it("일부 필드만 보낸 PATCH 는 나머지 기존 값을 유지한다") {
                val account = AccountTestFixture.createAccount(id = accountId, currentBalance = 100_000)
                val transaction = appliedTransaction(account, AccountTransactionType.EXPENSE, 30_000)
                every { accountRepository.findByIdWithLock(accountId) } returns account
                every { transactionRepository.findByIdAndDeletedAtIsNull(transactionId) } returns transaction
                // title 만 변경, 나머지는 null=변경 안 함
                val request = UpdateAccountTransactionRequest(title = "제목만 수정")

                val response = useCase.update(account.club.id, accountId, transactionId, request, userId)

                response.title shouldBe "제목만 수정"
                response.type shouldBe AccountTransactionType.EXPENSE
                response.amount shouldBe 30_000
                account.currentBalance shouldBe 70_000
            }

            it("시스템 거래(DUES) 수정 요청은 거부한다") {
                val account = AccountTestFixture.createAccount(id = accountId)
                val transaction = appliedTransaction(account, AccountTransactionType.DUES, 10_000)
                every { accountRepository.findByIdWithLock(accountId) } returns account
                every { transactionRepository.findByIdAndDeletedAtIsNull(transactionId) } returns transaction
                val request =
                    UpdateAccountTransactionRequest(AccountTransactionType.EXPENSE, 1_000, "수정", null, date, null)

                shouldThrow<AccountTransactionTypeNotAllowedException> {
                    useCase.update(account.club.id, accountId, transactionId, request, userId)
                }
            }

            it("존재하지 않는 거래면 NotFound를 던진다") {
                val account = AccountTestFixture.createAccount(id = accountId)
                every { accountRepository.findByIdWithLock(accountId) } returns account
                every { transactionRepository.findByIdAndDeletedAtIsNull(transactionId) } returns null
                val request =
                    UpdateAccountTransactionRequest(AccountTransactionType.EXPENSE, 1_000, "수정", null, date, null)

                shouldThrow<AccountTransactionNotFoundException> {
                    useCase.update(account.club.id, accountId, transactionId, request, userId)
                }
            }
        }

        describe("delete") {
            it("삭제 시 잔액을 원복하고 소프트 삭제한다") {
                val account = AccountTestFixture.createAccount(id = accountId, currentBalance = 100_000)
                val transaction = appliedTransaction(account, AccountTransactionType.EXPENSE, 30_000)
                account.currentBalance shouldBe 70_000
                every { accountRepository.findByIdWithLock(accountId) } returns account
                every { transactionRepository.findByIdAndDeletedAtIsNull(transactionId) } returns transaction

                useCase.delete(account.club.id, accountId, transactionId, userId)

                account.currentBalance shouldBe 100_000
                transaction.deletedAt.shouldNotBeNull()
            }

            it("시스템 거래(CARRY_OVER) 삭제 요청은 거부한다") {
                val account = AccountTestFixture.createAccount(id = accountId)
                val transaction = appliedTransaction(account, AccountTransactionType.CARRY_OVER, 10_000)
                every { accountRepository.findByIdWithLock(accountId) } returns account
                every { transactionRepository.findByIdAndDeletedAtIsNull(transactionId) } returns transaction

                shouldThrow<AccountTransactionTypeNotAllowedException> {
                    useCase.delete(account.club.id, accountId, transactionId, userId)
                }
            }
        }
    })
