package com.weeth.domain.account.presentation

import com.weeth.domain.account.application.dto.request.AccountSaveRequest
import com.weeth.domain.account.application.dto.request.SaveAccountBasicRequest
import com.weeth.domain.account.application.dto.request.SavePaymentTargetsRequest
import com.weeth.domain.account.application.dto.response.AccountPaymentTargetsResponse
import com.weeth.domain.account.application.dto.response.CreateAccountDraftResponse
import com.weeth.domain.account.application.exception.AccountErrorCode
import com.weeth.domain.account.application.usecase.command.ManageAccountUseCase
import com.weeth.domain.account.application.usecase.command.RegisterAccountUseCase
import com.weeth.domain.account.application.usecase.query.GetAccountPaymentTargetQueryService
import com.weeth.domain.account.domain.enums.AccountTargetStatus
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
    private val getAccountPaymentTargetQueryService: GetAccountPaymentTargetQueryService,
) {
    @PostMapping("/drafts")
    @Operation(
        summary = "[1단계] 회비 등록 초안 생성",
        description = "회비를 등록하기 전에 초안을 생성합니다. 멱등성 보장을 위해 이미 작성 중인 초안이 있다면 ID를 반환합니다. 총 회비 등록 전까지는 매번 호출해서 ID를 조회해주세요",
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
