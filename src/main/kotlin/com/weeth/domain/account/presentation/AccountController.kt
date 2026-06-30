package com.weeth.domain.account.presentation

import com.weeth.domain.account.application.dto.request.AccountTransactionFilter
import com.weeth.domain.account.application.dto.request.AccountTransactionSort
import com.weeth.domain.account.application.dto.response.AccountCardinalResponse
import com.weeth.domain.account.application.dto.response.MemberAccountTransactionsResponse
import com.weeth.domain.account.application.dto.response.MemberTransactionDetailResponse
import com.weeth.domain.account.application.dto.response.MyAccountResponse
import com.weeth.domain.account.application.exception.AccountErrorCode
import com.weeth.domain.account.application.usecase.query.GetMyAccountQueryService
import com.weeth.domain.account.application.usecase.query.GetMyAccountTransactionQueryService
import com.weeth.domain.account.presentation.AccountResponseCode.ACCOUNT_CARDINAL_LIST_FIND_SUCCESS
import com.weeth.domain.account.presentation.AccountResponseCode.ACCOUNT_MY_SUMMARY_FIND_SUCCESS
import com.weeth.domain.account.presentation.AccountResponseCode.ACCOUNT_TRANSACTION_DETAIL_FIND_SUCCESS
import com.weeth.domain.account.presentation.AccountResponseCode.ACCOUNT_TRANSACTION_FIND_SUCCESS
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import com.weeth.global.common.web.TsidParam
import com.weeth.global.common.web.TsidPathVariable
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "ACCOUNT", description = "회비 API")
@RestController
@RequestMapping("/api/v4/clubs/{clubId}/accounts")
@ApiErrorCodeExample(AccountErrorCode::class)
class AccountController(
    private val getMyAccountQueryService: GetMyAccountQueryService,
    private val getMyAccountTransactionQueryService: GetMyAccountTransactionQueryService,
) {
    @GetMapping("/cardinals")
    @Operation(summary = "부원 회비 기수 목록 조회")
    fun findCardinals(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<List<AccountCardinalResponse>> =
        CommonResponse.success(
            ACCOUNT_CARDINAL_LIST_FIND_SUCCESS,
            getMyAccountQueryService.findCardinals(clubId = clubId, userId = userId),
        )

    @GetMapping("/{cardinal}/me")
    @Operation(summary = "나의 회비 정보 조회")
    fun findMyAccount(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable cardinal: Int,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<MyAccountResponse> =
        CommonResponse.success(
            ACCOUNT_MY_SUMMARY_FIND_SUCCESS,
            getMyAccountQueryService.findMyAccount(clubId = clubId, cardinal = cardinal, userId = userId),
        )

    @GetMapping("/{cardinal}/transactions")
    @Operation(summary = "부원 회비 거래 내역 목록 조회")
    fun findTransactions(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable cardinal: Int,
        @RequestParam(defaultValue = "ALL") filter: AccountTransactionFilter,
        @RequestParam(defaultValue = "LATEST") sort: AccountTransactionSort,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<MemberAccountTransactionsResponse> =
        CommonResponse.success(
            ACCOUNT_TRANSACTION_FIND_SUCCESS,
            getMyAccountTransactionQueryService.findTransactions(
                clubId = clubId,
                cardinal = cardinal,
                filter = filter,
                sort = sort,
                page = page,
                size = size,
                userId = userId,
            ),
        )

    @GetMapping("/{cardinal}/transactions/{transactionId}")
    @Operation(summary = "부원 회비 거래 내역 상세 조회")
    fun findTransaction(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable cardinal: Int,
        @PathVariable transactionId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<MemberTransactionDetailResponse> =
        CommonResponse.success(
            ACCOUNT_TRANSACTION_DETAIL_FIND_SUCCESS,
            getMyAccountTransactionQueryService.findTransaction(
                clubId = clubId,
                cardinal = cardinal,
                transactionId = transactionId,
                userId = userId,
            ),
        )
}
