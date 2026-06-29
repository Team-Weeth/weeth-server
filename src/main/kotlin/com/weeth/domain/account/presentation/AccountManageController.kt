package com.weeth.domain.account.presentation

import com.weeth.domain.account.application.dto.request.AccountPaymentStatusFilter
import com.weeth.domain.account.application.dto.request.AccountTransactionFilter
import com.weeth.domain.account.application.dto.request.AccountTransactionSort
import com.weeth.domain.account.application.dto.request.MarkPaymentPaidRequest
import com.weeth.domain.account.application.dto.request.MarkPaymentUnpaidRequest
import com.weeth.domain.account.application.dto.request.RefundPaymentRequest
import com.weeth.domain.account.application.dto.request.SaveAccountTransactionRequest
import com.weeth.domain.account.application.dto.request.UpdateAccountTransactionRequest
import com.weeth.domain.account.application.dto.request.UpdateMemberVisibilityRequest
import com.weeth.domain.account.application.dto.response.AccountDashboardResponse
import com.weeth.domain.account.application.dto.response.AccountPaymentStatusResponse
import com.weeth.domain.account.application.dto.response.AccountTransactionResponse
import com.weeth.domain.account.application.dto.response.AccountTransactionsResponse
import com.weeth.domain.account.application.exception.AccountErrorCode
import com.weeth.domain.account.application.usecase.command.ManageAccountPaymentUseCase
import com.weeth.domain.account.application.usecase.command.ManageAccountTransactionUseCase
import com.weeth.domain.account.application.usecase.command.ManageAccountUseCase
import com.weeth.domain.account.application.usecase.query.GetAccountDashboardQueryService
import com.weeth.domain.account.application.usecase.query.GetAccountPaymentTargetQueryService
import com.weeth.domain.account.application.usecase.query.GetAccountTransactionQueryService
import com.weeth.domain.account.presentation.AccountResponseCode.ACCOUNT_DASHBOARD_FIND_SUCCESS
import com.weeth.domain.account.presentation.AccountResponseCode.ACCOUNT_PAYMENT_MARK_PAID_SUCCESS
import com.weeth.domain.account.presentation.AccountResponseCode.ACCOUNT_PAYMENT_MARK_UNPAID_SUCCESS
import com.weeth.domain.account.presentation.AccountResponseCode.ACCOUNT_PAYMENT_REFUND_SUCCESS
import com.weeth.domain.account.presentation.AccountResponseCode.ACCOUNT_PAYMENT_STATUS_FIND_SUCCESS
import com.weeth.domain.account.presentation.AccountResponseCode.ACCOUNT_TRANSACTION_DELETE_SUCCESS
import com.weeth.domain.account.presentation.AccountResponseCode.ACCOUNT_TRANSACTION_DETAIL_FIND_SUCCESS
import com.weeth.domain.account.presentation.AccountResponseCode.ACCOUNT_TRANSACTION_FIND_SUCCESS
import com.weeth.domain.account.presentation.AccountResponseCode.ACCOUNT_TRANSACTION_SAVE_SUCCESS
import com.weeth.domain.account.presentation.AccountResponseCode.ACCOUNT_TRANSACTION_UPDATE_SUCCESS
import com.weeth.domain.account.presentation.AccountResponseCode.ACCOUNT_UPDATE_SUCCESS
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import com.weeth.global.common.web.TsidParam
import com.weeth.global.common.web.TsidPathVariable
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "ACCOUNT ADMIN", description = "[ADMIN] 회비 어드민 API")
@RestController
@RequestMapping("/api/v4/admin/clubs/{clubId}/accounts")
@ApiErrorCodeExample(AccountErrorCode::class)
class AccountManageController(
    private val manageAccountUseCase: ManageAccountUseCase,
    private val manageAccountTransactionUseCase: ManageAccountTransactionUseCase,
    private val manageAccountPaymentUseCase: ManageAccountPaymentUseCase,
    private val getAccountTransactionQueryService: GetAccountTransactionQueryService,
    private val getAccountDashboardQueryService: GetAccountDashboardQueryService,
    private val getAccountPaymentTargetQueryService: GetAccountPaymentTargetQueryService,
) {
    @GetMapping("/{accountId}/dashboard")
    @Operation(summary = "회비 대시보드 조회", description = "잔액/총액, 납부 현황, 계좌, 월별 잔액 추이, 마지막 수정 정보를 집계해 반환합니다.")
    fun getDashboard(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable accountId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<AccountDashboardResponse> =
        CommonResponse.success(
            ACCOUNT_DASHBOARD_FIND_SUCCESS,
            getAccountDashboardQueryService.getDashboard(clubId, accountId, userId),
        )

    @GetMapping("/{accountId}/payment-status")
    @Operation(
        summary = "부원별 납부현황 조회",
        description = "회비관리 페이지의 상단 요약(수납액/목표/납부율/카운트/계좌)과 납부 대상 부원 목록(미납 순)을 조회합니다.",
    )
    fun findPaymentStatus(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable accountId: Long,
        @RequestParam(defaultValue = "ALL") paymentStatus: AccountPaymentStatusFilter,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<AccountPaymentStatusResponse> =
        CommonResponse.success(
            ACCOUNT_PAYMENT_STATUS_FIND_SUCCESS,
            getAccountPaymentTargetQueryService.findPaymentStatus(
                clubId = clubId,
                accountId = accountId,
                userId = userId,
                paymentStatusFilter = paymentStatus,
                keyword = keyword,
                page = page,
                size = size,
            ),
        )

    @PatchMapping("/{accountId}/member-visibility")
    @Operation(summary = "부원 거래 내역 공개 여부 수정")
    fun updateMemberVisibility(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable accountId: Long,
        @RequestBody @Valid request: UpdateMemberVisibilityRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void> {
        manageAccountUseCase.updateMemberVisibility(
            clubId = clubId,
            accountId = accountId,
            visible = request.visible,
            userId = userId,
        )
        return CommonResponse.success(ACCOUNT_UPDATE_SUCCESS)
    }

    @PostMapping("/{accountId}/transactions")
    @Operation(summary = "거래 내역 추가", description = "수입/지출 거래를 등록하고 장부 잔액에 반영합니다.")
    fun saveTransaction(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable accountId: Long,
        @RequestBody @Valid request: SaveAccountTransactionRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<AccountTransactionResponse> =
        CommonResponse.success(
            ACCOUNT_TRANSACTION_SAVE_SUCCESS,
            manageAccountTransactionUseCase.save(clubId, accountId, request, userId),
        )

    @GetMapping("/{accountId}/transactions")
    @Operation(summary = "거래 내역 목록 조회", description = "필터 탭(전체/수입/지출/회비)과 정렬, 페이지네이션을 지원합니다.")
    fun findTransactions(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable accountId: Long,
        @RequestParam(defaultValue = "ALL") filter: AccountTransactionFilter,
        @RequestParam(defaultValue = "LATEST") sort: AccountTransactionSort,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<AccountTransactionsResponse> =
        CommonResponse.success(
            ACCOUNT_TRANSACTION_FIND_SUCCESS,
            getAccountTransactionQueryService.findTransactions(
                clubId = clubId,
                accountId = accountId,
                filter = filter,
                sort = sort,
                page = page,
                size = size,
                userId = userId,
            ),
        )

    @GetMapping("/{accountId}/transactions/{transactionId}")
    @Operation(summary = "거래 내역 상세 조회")
    fun findTransaction(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable accountId: Long,
        @PathVariable transactionId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<AccountTransactionResponse> =
        CommonResponse.success(
            ACCOUNT_TRANSACTION_DETAIL_FIND_SUCCESS,
            getAccountTransactionQueryService.findTransaction(clubId, accountId, transactionId, userId),
        )

    @PatchMapping("/{accountId}/transactions/{transactionId}")
    @Operation(summary = "거래 내역 수정", description = "금액 변경 시 장부 잔액을 재계산합니다. 시스템 거래는 수정할 수 없습니다.")
    fun updateTransaction(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable accountId: Long,
        @PathVariable transactionId: Long,
        @RequestBody @Valid request: UpdateAccountTransactionRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<AccountTransactionResponse> =
        CommonResponse.success(
            ACCOUNT_TRANSACTION_UPDATE_SUCCESS,
            manageAccountTransactionUseCase.update(clubId, accountId, transactionId, request, userId),
        )

    @DeleteMapping("/{accountId}/transactions/{transactionId}")
    @Operation(summary = "거래 내역 삭제", description = "소프트 삭제하고 장부 잔액을 원복합니다. 시스템 거래는 삭제할 수 없습니다.")
    fun deleteTransaction(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable accountId: Long,
        @PathVariable transactionId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void> {
        manageAccountTransactionUseCase.delete(clubId, accountId, transactionId, userId)
        return CommonResponse.success(ACCOUNT_TRANSACTION_DELETE_SUCCESS)
    }

    @PatchMapping("/{accountId}/payment-targets/paid")
    @Operation(summary = "납부 확인(벌크)", description = "대상들을 납부 완료 처리하고 시스템 회비 수입 거래를 생성합니다.")
    fun markPaymentPaid(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable accountId: Long,
        @RequestBody @Valid request: MarkPaymentPaidRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void> {
        manageAccountPaymentUseCase.markPaid(clubId, accountId, request, userId)
        return CommonResponse.success(ACCOUNT_PAYMENT_MARK_PAID_SUCCESS)
    }

    @PatchMapping("/{accountId}/payment-targets/unpaid")
    @Operation(summary = "납부 정정(벌크)", description = "잘못 확인한 납부를 취소하고 해당 회비 거래를 원복합니다.")
    fun markPaymentUnpaid(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable accountId: Long,
        @RequestBody @Valid request: MarkPaymentUnpaidRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void> {
        manageAccountPaymentUseCase.markUnpaid(clubId, accountId, request, userId)
        return CommonResponse.success(ACCOUNT_PAYMENT_MARK_UNPAID_SUCCESS)
    }

    @PatchMapping("/{accountId}/payment-targets/refund")
    @Operation(summary = "환불(벌크)", description = "납부 완료 대상을 환불 처리하고 시스템 환불 지출 거래를 생성합니다. 납부 이력은 보존됩니다.")
    fun refundPayment(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable accountId: Long,
        @RequestBody @Valid request: RefundPaymentRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void> {
        manageAccountPaymentUseCase.refund(clubId, accountId, request, userId)
        return CommonResponse.success(ACCOUNT_PAYMENT_REFUND_SUCCESS)
    }
}
