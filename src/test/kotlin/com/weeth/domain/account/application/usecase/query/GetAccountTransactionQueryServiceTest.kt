package com.weeth.domain.account.application.usecase.query

import com.weeth.domain.account.application.dto.request.AccountTransactionFilter
import com.weeth.domain.account.application.dto.request.AccountTransactionSort
import com.weeth.domain.account.application.exception.AccountNotActiveException
import com.weeth.domain.account.application.exception.AccountTransactionNotFoundException
import com.weeth.domain.account.application.mapper.AccountTransactionMapper
import com.weeth.domain.account.domain.entity.Account
import com.weeth.domain.account.domain.entity.AccountTransaction
import com.weeth.domain.account.domain.enums.AccountStatus
import com.weeth.domain.account.domain.enums.AccountTransactionDirection
import com.weeth.domain.account.domain.enums.AccountTransactionType
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.account.domain.repository.AccountTransactionRepository
import com.weeth.domain.account.domain.vo.Money
import com.weeth.domain.account.fixture.AccountTestFixture
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.file.application.dto.response.FileResponse
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.enums.FileStatus
import com.weeth.domain.file.domain.repository.FileReader
import com.weeth.domain.file.domain.vo.StorageKey
import com.weeth.domain.file.fixture.FileTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate
import java.util.Optional

class GetAccountTransactionQueryServiceTest :
    DescribeSpec({
        val accountRepository = mockk<AccountRepository>()
        val transactionRepository = mockk<AccountTransactionRepository>(relaxed = true)
        val clubPermissionPolicy = mockk<ClubPermissionPolicy>(relaxed = true)
        val fileReader = mockk<FileReader>()
        val fileMapper = mockk<FileMapper>()
        val queryService =
            GetAccountTransactionQueryService(
                accountRepository,
                transactionRepository,
                clubPermissionPolicy,
                fileReader,
                fileMapper,
                AccountTransactionMapper(),
            )

        val userId = 10L
        val accountId = 1L
        val receiptResponse =
            FileResponse(
                fileId = 200L,
                fileName = "receipt.png",
                fileUrl = "https://cdn.weeth/receipt.png",
                storageKey = "ACCOUNT_TRANSACTION/2026-07/550e8400-e29b-41d4-a716-446655440200_receipt.png",
                fileSize = 1024,
                contentType = "image/png",
                status = FileStatus.UPLOADED,
            )

        fun receiptFile(ownerId: Long) =
            FileTestFixture.createFile(
                id = 200L,
                fileName = "receipt.png",
                storageKey = StorageKey(receiptResponse.storageKey),
                ownerType = FileOwnerType.ACCOUNT_TRANSACTION,
                ownerId = ownerId,
            )

        fun transaction(
            account: Account,
            id: Long,
            type: AccountTransactionType,
            amount: Int,
            balanceAfter: Int = 0,
        ): AccountTransaction =
            AccountTransaction
                .create(
                    account = account,
                    type = type,
                    title = "거래",
                    source = null,
                    amount = Money.of(amount),
                    transactedAt = LocalDate.of(2026, 7, 20).atStartOfDay(),
                ).also {
                    ReflectionTestUtils.setField(it, "id", id)
                    ReflectionTestUtils.setField(it, "balanceAfter", balanceAfter)
                }

        beforeTest {
            clearMocks(accountRepository, transactionRepository, clubPermissionPolicy, fileReader, fileMapper)
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
            every { fileReader.findAll(FileOwnerType.ACCOUNT_TRANSACTION, any<List<Long>>(), any()) } returns
                emptyList()
            every { fileReader.findAll(FileOwnerType.ACCOUNT_TRANSACTION, any<Long>(), any()) } returns emptyList()
            every { fileMapper.toFileResponse(any()) } returns receiptResponse
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

            it("목록 거래의 영수증 여부를 배치 조회로 채운다") {
                val account = AccountTestFixture.createAccount(id = accountId)
                val transactions =
                    listOf(
                        transaction(account, 100L, AccountTransactionType.EXPENSE, 5_000),
                        transaction(account, 101L, AccountTransactionType.INCOME, 10_000),
                    )
                every { accountRepository.findById(accountId) } returns Optional.of(account)
                every { transactionRepository.findByAccountIdAndDeletedAtIsNull(accountId, any()) } returns
                    PageImpl(transactions)
                every {
                    fileReader.findAll(FileOwnerType.ACCOUNT_TRANSACTION, listOf(100L, 101L), any())
                } returns listOf(receiptFile(ownerId = 100L))

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

                result.transactions.content[0].hasReceipt shouldBe true
                result.transactions.content[0].receipts shouldBe emptyList()
                result.transactions.content[1].hasReceipt shouldBe false
                result.transactions.content[1].receipts shouldBe emptyList()
                verify(exactly = 1) {
                    fileReader.findAll(FileOwnerType.ACCOUNT_TRANSACTION, listOf(100L, 101L), any())
                }
                verify(exactly = 0) { fileReader.findAll(FileOwnerType.ACCOUNT_TRANSACTION, 100L, any()) }
                verify(exactly = 0) { fileReader.findAll(FileOwnerType.ACCOUNT_TRANSACTION, 101L, any()) }
            }

            it("각 거래에 저장된 시점 총잔액(balanceAfter)을 그대로 반환한다") {
                val account = AccountTestFixture.createAccount(id = accountId)
                val transactions =
                    listOf(
                        transaction(account, 100L, AccountTransactionType.EXPENSE, 5_000, balanceAfter = 15_000),
                        transaction(account, 101L, AccountTransactionType.INCOME, 10_000, balanceAfter = 25_000),
                    )
                every { accountRepository.findById(accountId) } returns Optional.of(account)
                every { transactionRepository.findByAccountIdAndDeletedAtIsNull(accountId, any()) } returns
                    PageImpl(transactions)

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

                result.transactions.content[0].balanceAfter shouldBe 15_000
                result.transactions.content[1].balanceAfter shouldBe 25_000
            }

            it("거래 일시는 날짜만 반환한다") {
                val account = AccountTestFixture.createAccount(id = accountId)
                val tx = transaction(account, 100L, AccountTransactionType.EXPENSE, 5_000)
                every { accountRepository.findById(accountId) } returns Optional.of(account)
                every { transactionRepository.findByAccountIdAndDeletedAtIsNull(accountId, any()) } returns
                    PageImpl(listOf(tx))

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
                    .transactedAt shouldBe LocalDate.of(2026, 7, 20)
            }

            it("LATEST 정렬은 거래 일자가 같을 때 생성일 최신순으로 조회한다") {
                val account = AccountTestFixture.createAccount(id = accountId)
                every { accountRepository.findById(accountId) } returns Optional.of(account)
                every { transactionRepository.findByAccountIdAndDeletedAtIsNull(accountId, any()) } answers {
                    val pageable = secondArg<Pageable>()
                    val orders = pageable.sort.toList()

                    orders[0].property shouldBe "transactedAt"
                    orders[0].direction shouldBe Sort.Direction.DESC
                    orders[1].property shouldBe "createdAt"
                    orders[1].direction shouldBe Sort.Direction.DESC

                    PageImpl(emptyList(), pageable, 0)
                }

                queryService.findTransactions(
                    account.club.id,
                    accountId,
                    AccountTransactionFilter.ALL,
                    AccountTransactionSort.LATEST,
                    0,
                    20,
                    userId,
                )
            }

            it("DRAFT 장부면 AccountNotActiveException 을 던진다") {
                val account = AccountTestFixture.createAccount(id = accountId, status = AccountStatus.DRAFT)
                every { accountRepository.findById(accountId) } returns Optional.of(account)

                shouldThrow<AccountNotActiveException> {
                    queryService.findTransactions(
                        account.club.id,
                        accountId,
                        AccountTransactionFilter.ALL,
                        AccountTransactionSort.LATEST,
                        0,
                        20,
                        userId,
                    )
                }
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
                    transaction(account, 100L, AccountTransactionType.EXPENSE, 5_000, balanceAfter = 15_000)

                val result = queryService.findTransaction(account.club.id, accountId, 100L, userId)

                result.transactionId shouldBe 100L
                result.amount shouldBe 5_000
                result.balanceAfter shouldBe 15_000
            }

            it("단건 상세에 영수증 파일 응답을 포함한다") {
                val account = AccountTestFixture.createAccount(id = accountId)
                val receipt = receiptFile(ownerId = 100L)
                every { accountRepository.findById(accountId) } returns Optional.of(account)
                every { transactionRepository.findByIdAndDeletedAtIsNull(100L) } returns
                    transaction(account, 100L, AccountTransactionType.EXPENSE, 5_000)
                every { fileReader.findAll(FileOwnerType.ACCOUNT_TRANSACTION, 100L, any()) } returns listOf(receipt)

                val result = queryService.findTransaction(account.club.id, accountId, 100L, userId)

                result.hasReceipt shouldBe true
                result.receipts shouldBe listOf(receiptResponse)
            }

            it("없는 거래면 NotFound를 던진다") {
                val account = AccountTestFixture.createAccount(id = accountId)
                every { accountRepository.findById(accountId) } returns Optional.of(account)
                every { transactionRepository.findByIdAndDeletedAtIsNull(999L) } returns null

                shouldThrow<AccountTransactionNotFoundException> {
                    queryService.findTransaction(account.club.id, accountId, 999L, userId)
                }
            }

            it("DRAFT 장부면 AccountNotActiveException 을 던진다") {
                val account = AccountTestFixture.createAccount(id = accountId, status = AccountStatus.DRAFT)
                every { accountRepository.findById(accountId) } returns Optional.of(account)

                shouldThrow<AccountNotActiveException> {
                    queryService.findTransaction(account.club.id, accountId, 100L, userId)
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
