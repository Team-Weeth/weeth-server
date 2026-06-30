package com.weeth.domain.account.application.usecase.query

import com.weeth.domain.account.application.dto.request.AccountTransactionFilter
import com.weeth.domain.account.application.dto.request.AccountTransactionSort
import com.weeth.domain.account.application.exception.AccountTransactionNotFoundException
import com.weeth.domain.account.application.mapper.MemberTransactionMapper
import com.weeth.domain.account.application.usecase.MemberAccountAccessResolver
import com.weeth.domain.account.domain.entity.Account
import com.weeth.domain.account.domain.entity.AccountPaymentTarget
import com.weeth.domain.account.domain.entity.AccountTransaction
import com.weeth.domain.account.domain.enums.AccountStatus
import com.weeth.domain.account.domain.enums.AccountTransactionType
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.account.domain.repository.AccountTransactionRepository
import com.weeth.domain.account.domain.vo.Money
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.file.application.dto.response.FileResponse
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.enums.FileStatus
import com.weeth.domain.file.domain.repository.FileReader
import com.weeth.domain.file.domain.vo.StorageKey
import com.weeth.domain.file.fixture.FileTestFixture
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime

class GetMyAccountTransactionQueryServiceTest :
    DescribeSpec({
        val accountRepository = mockk<AccountRepository>()
        val transactionRepository = mockk<AccountTransactionRepository>()
        val clubMemberPolicy = mockk<ClubMemberPolicy>()
        val fileReader = mockk<FileReader>()
        val fileMapper = mockk<FileMapper>()
        val queryService =
            GetMyAccountTransactionQueryService(
                transactionRepository = transactionRepository,
                memberAccountAccessResolver = MemberAccountAccessResolver(accountRepository, clubMemberPolicy),
                fileReader = fileReader,
                fileMapper = fileMapper,
                memberTransactionMapper = MemberTransactionMapper(),
            )

        val club = ClubTestFixture.createClub(id = 1L)
        val userId = 10L
        val accountId = 12L
        val member =
            ClubMemberTestFixture.createActiveMember(
                id = 50L,
                club = club,
                user = UserTestFixture.createActiveUser1(id = userId),
            )
        val otherMember =
            ClubMemberTestFixture.createActiveMember(
                id = 51L,
                club = club,
                user = UserTestFixture.createActiveUser2(id = 11L),
            )
        val account =
            Account(
                id = accountId,
                club = club,
                cardinal = 7,
                name = "7기 회비",
                duesAmount = 60_000,
                currentBalance = 152_129,
                memberVisible = true,
                status = AccountStatus.ACTIVE,
            )
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

        fun target(
            member: ClubMember,
            id: Long,
        ): AccountPaymentTarget =
            AccountPaymentTarget
                .createTargeted(account = account, clubMember = member, dueAmount = Money.of(60_000))
                .also { ReflectionTestUtils.setField(it, "id", id) }

        fun transaction(
            id: Long,
            type: AccountTransactionType,
            source: String? = "출처",
            paymentTarget: AccountPaymentTarget? = null,
            registeredByName: String? = "운영진 김검도",
        ): AccountTransaction =
            AccountTransaction
                .create(
                    account = account,
                    type = type,
                    title = "거래",
                    source = source,
                    amount = Money.of(30_000),
                    transactedAt = LocalDateTime.of(2026, 7, 20, 0, 0),
                    memo = "메모",
                    paymentTarget = paymentTarget,
                    registeredByName = registeredByName,
                ).also { ReflectionTestUtils.setField(it, "id", id) }

        fun receiptFile(ownerId: Long) =
            FileTestFixture.createFile(
                id = 200L,
                fileName = "receipt.png",
                storageKey = StorageKey(receiptResponse.storageKey),
                ownerType = FileOwnerType.ACCOUNT_TRANSACTION,
                ownerId = ownerId,
            )

        beforeTest {
            clearMocks(accountRepository, transactionRepository, clubMemberPolicy, fileReader, fileMapper)
            every { clubMemberPolicy.getActiveMember(club.id, userId) } returns member
            every {
                accountRepository.findByClubIdAndCardinalAndStatusAndMemberVisibleTrue(
                    club.id,
                    7,
                    AccountStatus.ACTIVE,
                )
            } returns account
            every { transactionRepository.sumNetDuesAmountByAccountId(accountId) } returns 110_000L
            every {
                transactionRepository.countMemberVisibleTransactions(
                    accountId = accountId,
                    clubMemberId = 50L,
                    publicTypes =
                        listOf(
                            AccountTransactionType.INCOME,
                            AccountTransactionType.EXPENSE,
                            AccountTransactionType.CARRY_OVER,
                        ),
                    includeRefund = true,
                )
            } returns 4
            every {
                transactionRepository.countMemberVisibleTransactions(
                    accountId = accountId,
                    clubMemberId = 50L,
                    publicTypes = listOf(AccountTransactionType.INCOME),
                    includeRefund = false,
                )
            } returns 1
            every {
                transactionRepository.countMemberVisibleTransactions(
                    accountId = accountId,
                    clubMemberId = 50L,
                    publicTypes = listOf(AccountTransactionType.EXPENSE),
                    includeRefund = true,
                )
            } returns 3
            every { fileReader.findAll(FileOwnerType.ACCOUNT_TRANSACTION, any<List<Long>>(), any()) } returns
                emptyList()
            every { fileReader.findAll(FileOwnerType.ACCOUNT_TRANSACTION, any<Long>(), any()) } returns emptyList()
            every { fileMapper.toFileResponse(any()) } returns receiptResponse
        }

        describe("findTransactions") {
            it("ALL 목록은 DUES를 제외하고 공개 거래와 내 REFUND만 반환하며 REFUND source를 마스킹한다") {
                val ownRefund = transaction(101L, AccountTransactionType.REFUND, "홍길동", target(member, 1L))
                val manualIncome = transaction(102L, AccountTransactionType.INCOME, "후원")
                every {
                    transactionRepository.findMemberVisibleTransactions(
                        accountId = accountId,
                        clubMemberId = 50L,
                        publicTypes =
                            listOf(
                                AccountTransactionType.INCOME,
                                AccountTransactionType.EXPENSE,
                                AccountTransactionType.CARRY_OVER,
                            ),
                        includeRefund = true,
                        pageable = any(),
                    )
                } returns PageImpl(listOf(ownRefund, manualIncome))

                val result =
                    queryService.findTransactions(
                        clubId = club.id,
                        cardinal = 7,
                        filter = AccountTransactionFilter.ALL,
                        sort = AccountTransactionSort.LATEST,
                        page = 0,
                        size = 20,
                        userId = userId,
                    )

                result.duesSummary.shouldNotBeNull().totalAmount shouldBe 110_000
                result.counts.shouldNotBeNull().all shouldBe 4
                result.counts.shouldNotBeNull().dues shouldBe 1
                result.transactions.content.map { it.transactionId } shouldBe listOf(101L, 102L)
                result.transactions.content
                    .first()
                    .source shouldBe "환불"
            }

            it("DUES 필터는 개별 거래 없이 duesSummary만 반환한다") {
                val result =
                    queryService.findTransactions(
                        clubId = club.id,
                        cardinal = 7,
                        filter = AccountTransactionFilter.DUES,
                        sort = AccountTransactionSort.LATEST,
                        page = 0,
                        size = 20,
                        userId = userId,
                    )

                result.duesSummary.shouldNotBeNull().totalAmount shouldBe 110_000
                result.transactions.content.shouldBeEmpty()
                verify(exactly = 0) {
                    transactionRepository.findMemberVisibleTransactions(any(), any(), any(), any(), any())
                }
            }

            it("첫 페이지가 아니면 counts·duesSummary를 생략하고 집계 쿼리를 호출하지 않는다") {
                every {
                    transactionRepository.findMemberVisibleTransactions(
                        accountId = accountId,
                        clubMemberId = 50L,
                        publicTypes =
                            listOf(
                                AccountTransactionType.INCOME,
                                AccountTransactionType.EXPENSE,
                                AccountTransactionType.CARRY_OVER,
                            ),
                        includeRefund = true,
                        pageable = any(),
                    )
                } returns PageImpl(listOf(transaction(103L, AccountTransactionType.EXPENSE)))

                val result =
                    queryService.findTransactions(
                        clubId = club.id,
                        cardinal = 7,
                        filter = AccountTransactionFilter.ALL,
                        sort = AccountTransactionSort.LATEST,
                        page = 1,
                        size = 20,
                        userId = userId,
                    )

                result.counts.shouldBeNull()
                result.duesSummary.shouldBeNull()
                result.transactions.content.map { it.transactionId } shouldBe listOf(103L)
                verify(exactly = 0) { transactionRepository.sumNetDuesAmountByAccountId(any()) }
                verify(exactly = 0) {
                    transactionRepository.countMemberVisibleTransactions(any(), any(), any(), any())
                }
            }

            it("목록의 hasReceipt는 거래 ID 목록으로 배치 조회한다") {
                val expense = transaction(101L, AccountTransactionType.EXPENSE)
                val income = transaction(102L, AccountTransactionType.INCOME)
                every {
                    transactionRepository.findMemberVisibleTransactions(
                        accountId = accountId,
                        clubMemberId = 50L,
                        publicTypes =
                            listOf(
                                AccountTransactionType.INCOME,
                                AccountTransactionType.EXPENSE,
                                AccountTransactionType.CARRY_OVER,
                            ),
                        includeRefund = true,
                        pageable = any(),
                    )
                } returns PageImpl(listOf(expense, income))
                every {
                    fileReader.findAll(FileOwnerType.ACCOUNT_TRANSACTION, listOf(101L, 102L), any())
                } returns listOf(receiptFile(ownerId = 101L))

                val result =
                    queryService.findTransactions(
                        clubId = club.id,
                        cardinal = 7,
                        filter = AccountTransactionFilter.ALL,
                        sort = AccountTransactionSort.LATEST,
                        page = 0,
                        size = 20,
                        userId = userId,
                    )

                result.transactions.content[0].hasReceipt shouldBe true
                result.transactions.content[1].hasReceipt shouldBe false
                verify(exactly = 1) {
                    fileReader.findAll(FileOwnerType.ACCOUNT_TRANSACTION, listOf(101L, 102L), any())
                }
            }
        }

        describe("findTransaction") {
            it("수동 거래 상세는 영수증과 등록자 이름 스냅샷을 반환한다") {
                val expense = transaction(101L, AccountTransactionType.EXPENSE, registeredByName = "운영진 김검도")
                every { transactionRepository.findByIdAndDeletedAtIsNull(101L) } returns expense
                every { fileReader.findAll(FileOwnerType.ACCOUNT_TRANSACTION, 101L, any()) } returns
                    listOf(receiptFile(ownerId = 101L))

                val result = queryService.findTransaction(club.id, cardinal = 7, transactionId = 101L, userId = userId)

                result.transactionId shouldBe 101L
                result.registeredByName shouldBe "운영진 김검도"
                result.hasReceipt shouldBe true
                result.receipts shouldBe listOf(receiptResponse)
            }

            it("등록자 이름이 없으면 운영진으로 fallback한다") {
                val expense = transaction(101L, AccountTransactionType.EXPENSE, registeredByName = null)
                every { transactionRepository.findByIdAndDeletedAtIsNull(101L) } returns expense

                val result = queryService.findTransaction(club.id, cardinal = 7, transactionId = 101L, userId = userId)

                result.registeredByName shouldBe "운영진"
            }

            it("내 REFUND 상세는 source를 환불로 마스킹한다") {
                val ownRefund = transaction(101L, AccountTransactionType.REFUND, "홍길동", target(member, 1L))
                every { transactionRepository.findByIdAndDeletedAtIsNull(101L) } returns ownRefund

                val result = queryService.findTransaction(club.id, cardinal = 7, transactionId = 101L, userId = userId)

                result.source shouldBe "환불"
            }

            it("DUES 상세 요청은 NotFound로 은닉한다") {
                every { transactionRepository.findByIdAndDeletedAtIsNull(101L) } returns
                    transaction(101L, AccountTransactionType.DUES, "홍길동", target(member, 1L))

                shouldThrow<AccountTransactionNotFoundException> {
                    queryService.findTransaction(club.id, cardinal = 7, transactionId = 101L, userId = userId)
                }
            }

            it("다른 부원의 REFUND 상세 요청은 NotFound로 은닉한다") {
                every { transactionRepository.findByIdAndDeletedAtIsNull(101L) } returns
                    transaction(101L, AccountTransactionType.REFUND, "다른부원", target(otherMember, 2L))

                shouldThrow<AccountTransactionNotFoundException> {
                    queryService.findTransaction(club.id, cardinal = 7, transactionId = 101L, userId = userId)
                }
            }
        }
    })
