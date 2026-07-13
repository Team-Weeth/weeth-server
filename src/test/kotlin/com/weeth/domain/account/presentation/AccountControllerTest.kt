package com.weeth.domain.account.presentation

import com.weeth.domain.account.application.dto.request.AccountTransactionFilter
import com.weeth.domain.account.application.dto.request.AccountTransactionSort
import com.weeth.domain.account.application.dto.response.AccountCardinalResponse
import com.weeth.domain.account.application.dto.response.AccountVisibilityResponse
import com.weeth.domain.account.application.dto.response.MemberAccountTransactionsResponse
import com.weeth.domain.account.application.dto.response.MemberTransactionDetailResponse
import com.weeth.domain.account.application.dto.response.MyAccountResponse
import com.weeth.domain.account.application.usecase.query.GetMyAccountQueryService
import com.weeth.domain.account.application.usecase.query.GetMyAccountTransactionQueryService
import com.weeth.domain.account.domain.enums.AccountTransactionDirection
import com.weeth.domain.account.domain.enums.AccountTransactionType
import com.weeth.global.common.response.SliceResponse
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class AccountControllerTest :
    DescribeSpec({
        val getMyAccountQueryService = mockk<GetMyAccountQueryService>()
        val getMyAccountTransactionQueryService = mockk<GetMyAccountTransactionQueryService>()
        val controller =
            AccountController(
                getMyAccountQueryService = getMyAccountQueryService,
                getMyAccountTransactionQueryService = getMyAccountTransactionQueryService,
            )

        val clubId = 1L
        val userId = 10L

        beforeTest {
            clearMocks(getMyAccountQueryService, getMyAccountTransactionQueryService)
        }

        describe("getVisibility") {
            it("회비 기능 공개 여부 성공 코드를 반환한다") {
                every { getMyAccountQueryService.getVisibility(clubId, userId) } returns
                    AccountVisibilityResponse(visible = true)

                val response = controller.getVisibility(clubId, userId)

                response.code shouldBe AccountResponseCode.ACCOUNT_VISIBILITY_FIND_SUCCESS.code
                response.data?.visible shouldBe true
            }
        }

        describe("findCardinals") {
            it("회비 기수 목록 성공 코드를 반환한다") {
                val data = listOf(AccountCardinalResponse(cardinal = 7, name = "7기 회비", isLatest = true))
                every { getMyAccountQueryService.findCardinals(clubId, userId) } returns data

                val response = controller.findCardinals(clubId, userId)

                response.code shouldBe AccountResponseCode.ACCOUNT_CARDINAL_LIST_FIND_SUCCESS.code
                response.data shouldBe data
            }
        }

        describe("findMyAccount") {
            it("나의 회비 정보 성공 코드를 반환한다") {
                val data =
                    MyAccountResponse(
                        accountId = 12L,
                        cardinal = 7,
                        accountName = "7기 회비",
                        duesAmount = 60_000,
                        myPayment =
                            MyAccountResponse.MyPaymentResponse(
                                targeted = false,
                                status = null,
                                dueAmount = 0,
                                paidAmount = 0,
                                paidAt = null,
                            ),
                        bankAccountVisible = false,
                        bankAccount = null,
                        balance = MyAccountResponse.BalanceResponse(currentBalance = 152_129, goalAmount = 1_425_000),
                    )
                every { getMyAccountQueryService.findMyAccount(clubId, 7, userId) } returns data

                val response = controller.findMyAccount(clubId, cardinal = 7, userId = userId)

                response.code shouldBe AccountResponseCode.ACCOUNT_MY_SUMMARY_FIND_SUCCESS.code
                response.data shouldBe data
            }
        }

        describe("findTransactions") {
            it("부원 거래 목록 성공 코드를 반환하고 필터/정렬을 전달한다") {
                val data =
                    MemberAccountTransactionsResponse(
                        counts =
                            MemberAccountTransactionsResponse.TransactionCountsResponse(
                                all = 0,
                                expense = 0,
                                income = 0,
                                dues = 0,
                            ),
                        duesSummary = MemberAccountTransactionsResponse.DuesSummaryResponse(totalAmount = 0),
                        transactions = SliceResponse(emptyList(), 0, 20, 0, false),
                    )
                every {
                    getMyAccountTransactionQueryService.findTransactions(
                        clubId,
                        7,
                        AccountTransactionFilter.ALL,
                        AccountTransactionSort.LATEST,
                        0,
                        20,
                        userId,
                    )
                } returns data

                val response =
                    controller.findTransactions(
                        clubId = clubId,
                        cardinal = 7,
                        filter = AccountTransactionFilter.ALL,
                        sort = AccountTransactionSort.LATEST,
                        page = 0,
                        size = 20,
                        userId = userId,
                    )

                response.code shouldBe AccountResponseCode.ACCOUNT_TRANSACTION_FIND_SUCCESS.code
                response.data shouldBe data
                verify(exactly = 1) {
                    getMyAccountTransactionQueryService.findTransactions(
                        clubId,
                        7,
                        AccountTransactionFilter.ALL,
                        AccountTransactionSort.LATEST,
                        0,
                        20,
                        userId,
                    )
                }
            }
        }

        describe("findTransaction") {
            it("부원 거래 상세 성공 코드를 반환한다") {
                val data =
                    MemberTransactionDetailResponse(
                        transactionId = 100L,
                        type = AccountTransactionType.EXPENSE,
                        direction = AccountTransactionDirection.EXPENSE,
                        title = "스터디 지원금",
                        source = "인프런",
                        amount = 50_000,
                        transactedAt = LocalDateTime.of(2026, 7, 20, 0, 0),
                        category = null,
                        registeredByName = "운영진",
                        memo = null,
                    )
                every { getMyAccountTransactionQueryService.findTransaction(clubId, 7, 100L, userId) } returns data

                val response = controller.findTransaction(clubId, cardinal = 7, transactionId = 100L, userId = userId)

                response.code shouldBe AccountResponseCode.ACCOUNT_TRANSACTION_DETAIL_FIND_SUCCESS.code
                response.data shouldBe data
            }
        }
    })
