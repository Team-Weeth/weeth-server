package com.weeth.domain.account.presentation

import com.weeth.domain.account.application.dto.request.AccountSaveRequest
import com.weeth.domain.account.application.dto.request.SaveAccountBankAccountRequest
import com.weeth.domain.account.application.dto.request.SaveAccountBasicRequest
import com.weeth.domain.account.application.dto.request.SaveAccountCarryOverRequest
import com.weeth.domain.account.application.dto.request.SavePaymentTargetsRequest
import com.weeth.domain.account.application.dto.response.AccountCarryOverSourceResponse
import com.weeth.domain.account.application.dto.response.AccountPaymentTargetsResponse
import com.weeth.domain.account.application.dto.response.CreateAccountDraftResponse
import com.weeth.domain.account.application.exception.AccountErrorCode
import com.weeth.domain.account.application.usecase.command.ManageAccountUseCase
import com.weeth.domain.account.application.usecase.command.RegisterAccountUseCase
import com.weeth.domain.account.application.usecase.query.GetAccountPaymentTargetQueryService
import com.weeth.domain.account.application.usecase.query.GetAccountRegistrationQueryService
import com.weeth.domain.account.domain.enums.AccountTargetStatus
import com.weeth.domain.account.presentation.AccountResponseCode.ACCOUNT_CARRY_OVER_SOURCE_FIND_SUCCESS
import com.weeth.domain.account.presentation.AccountResponseCode.ACCOUNT_DRAFT_DELETE_SUCCESS
import com.weeth.domain.account.presentation.AccountResponseCode.ACCOUNT_DRAFT_SAVE_SUCCESS
import com.weeth.domain.account.presentation.AccountResponseCode.ACCOUNT_PAYMENT_TARGET_FIND_SUCCESS
import com.weeth.domain.account.presentation.AccountResponseCode.ACCOUNT_PAYMENT_TARGET_UPDATE_SUCCESS
import com.weeth.domain.account.presentation.AccountResponseCode.ACCOUNT_SAVE_SUCCESS
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
class AccountRegisterController(
    private val manageAccountUseCase: ManageAccountUseCase,
    private val registerAccountUseCase: RegisterAccountUseCase,
    private val getAccountRegistrationQueryService: GetAccountRegistrationQueryService,
    private val getAccountPaymentTargetQueryService: GetAccountPaymentTargetQueryService,
) {
    @PostMapping("/drafts")
    @Operation(
        summary = "[1단계] 회비 등록 초안 생성",
        description =
            "회비를 등록하기 전에 초안을 생성합니다. 멱등성 보장을 위해 이미 작성 중인 초안이 있다면 ID를 반환합니다. " +
                "총 회비 등록 전까지는 매번 호출해서 ID를 조회해주세요. " +
                "isNew=false면 '이어서 작성 / 새로 작성' 분기를 노출하고, 이어서 작성 시 등록 현황 조회 API로 폼을 복원해주세요. " +
                "새로 작성 시에는 초안 폐기 API 호출 후 본 API를 재호출해주세요.",
    )
    fun createDraft(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @RequestParam cardinalNumber: Int,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<CreateAccountDraftResponse> =
        CommonResponse.success(
            ACCOUNT_DRAFT_SAVE_SUCCESS,
            registerAccountUseCase.createDraft(clubId = clubId, cardinal = cardinalNumber, userId = userId),
        )

    @DeleteMapping("/{accountId}/registration/draft")
    @Operation(summary = "회비 등록 초안 폐기", description = "작성 중인 DRAFT 상태 초안을 삭제합니다. 새로 작성하기 버튼에서 사용하세요.")
    fun discardDraft(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable accountId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void> {
        registerAccountUseCase.discardDraft(clubId = clubId, accountId = accountId, userId = userId)
        return CommonResponse.success(ACCOUNT_DRAFT_DELETE_SUCCESS)
    }

    @PatchMapping("/{accountId}/registration/basic")
    @Operation(summary = "[2단계] 회비 기본 정보 저장")
    fun saveBasic(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable accountId: Long,
        @RequestBody @Valid request: SaveAccountBasicRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void> {
        registerAccountUseCase.saveBasic(clubId = clubId, accountId = accountId, request = request, userId = userId)
        return CommonResponse.success(ACCOUNT_UPDATE_SUCCESS)
    }

    @GetMapping("/{accountId}/payment-targets")
    @Operation(
        summary = "회비 납부 대상 목록 조회",
        description =
            "등록 플로우 복원과 최종 확인에서 납부 대상/제외 대상 목록을 조회합니다. " +
                "각 행의 targetStatus(TARGETED/EXCLUDED)가 체크박스 초기 상태이며, " +
                "이후 사용자가 변경한 멤버만 모아 납부 대상 저장 API에 델타로 전달해주세요. " +
                "키워드와 상태 필터링도 가능하도록 했으나, 되도록 프론트에서 캐싱된 데이터로 필터링해주시면 감사하겠습니다.",
    )
    fun findPaymentTargets(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable accountId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) targetStatus: AccountTargetStatus?,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<AccountPaymentTargetsResponse> =
        CommonResponse.success(
            ACCOUNT_PAYMENT_TARGET_FIND_SUCCESS,
            getAccountPaymentTargetQueryService.findTargets(
                clubId = clubId,
                accountId = accountId,
                userId = userId,
                page = page,
                size = size,
                keyword = keyword,
                targetStatus = targetStatus,
            ),
        )

    @GetMapping("/{accountId}/registration/carry-over/source")
    @Operation(
        summary = "회비 이월 재원 조회",
        description =
            "이월 설정 단계 진입 시 직전 활성 기수 장부의 잔액을 조회합니다. " +
                "hasPreviousAccount=true면 'OO원 / 이전 기수 N기 잔액' 배너를 노출하고 이월 금액으로 balance를 사용해주세요. " +
                "false면 '이전 기수 정보가 없습니다' 안내와 함께 금액 직접 입력 UI를 노출해주세요.",
    )
    fun findCarryOverSource(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable accountId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<AccountCarryOverSourceResponse> =
        CommonResponse.success(
            ACCOUNT_CARRY_OVER_SOURCE_FIND_SUCCESS,
            getAccountRegistrationQueryService.findCarryOverSource(
                clubId = clubId,
                accountId = accountId,
                userId = userId,
            ),
        )

    @PatchMapping("/{accountId}/payment-targets")
    @Operation(
        summary = "[3단계] 회비 납부 대상 저장",
        description =
            "해당 기수 명부 기준으로 납부 대상을 델타 방식으로 저장합니다. " +
                "targetedClubMemberIds는 대상으로, excludedClubMemberIds는 제외로 갱신하며 " +
                "두 목록에 모두 없는 회원의 기존 상태는 유지됩니다. 초기 등록과 재설정 모두 동일하게 동작합니다.",
    )
    fun savePaymentTargets(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable accountId: Long,
        @RequestBody @Valid request: SavePaymentTargetsRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void> {
        registerAccountUseCase.savePaymentTargets(
            clubId = clubId,
            accountId = accountId,
            request = request,
            userId = userId,
        )
        return CommonResponse.success(ACCOUNT_PAYMENT_TARGET_UPDATE_SUCCESS)
    }

    @PatchMapping("/{accountId}/registration/carry-over")
    @Operation(
        summary = "[4단계] 회비 이월 설정 저장",
        description =
            "이월 여부와 금액, 메모를 저장합니다. 등록 완료 시 이전 기수 장부에 남은 잔액은 " +
                "이월하기면 '이월 잔액 전출', 이월하지 않기면 '미이월 잔액 정리' 명목의 지출 거래로 자동 정리됩니다. " +
                "이전 기수 잔액은 이월 재원 조회 API로 확인해주세요.",
    )
    fun saveCarryOver(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable accountId: Long,
        @RequestBody @Valid request: SaveAccountCarryOverRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void> {
        registerAccountUseCase.saveCarryOver(clubId = clubId, accountId = accountId, request = request, userId = userId)
        return CommonResponse.success(ACCOUNT_UPDATE_SUCCESS)
    }

    @PatchMapping("/{accountId}/registration/bank-account")
    @Operation(summary = "[5단계] 회비 계좌 설정 저장")
    fun saveBankAccount(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable accountId: Long,
        @RequestBody @Valid request: SaveAccountBankAccountRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void> {
        registerAccountUseCase.saveBankAccount(
            clubId = clubId,
            accountId = accountId,
            request = request,
            userId = userId,
        )
        return CommonResponse.success(ACCOUNT_UPDATE_SUCCESS)
    }

    @PostMapping
    @Operation(summary = "회비 총 금액 기입", hidden = true)
    fun save(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @RequestBody @Valid dto: AccountSaveRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void> {
        manageAccountUseCase.save(clubId, dto, userId)
        return CommonResponse.success(ACCOUNT_SAVE_SUCCESS)
    }
}
