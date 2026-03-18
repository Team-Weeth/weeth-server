package com.weeth.domain.account.presentation

import com.weeth.domain.account.application.dto.request.ReceiptSaveRequest
import com.weeth.domain.account.application.dto.request.ReceiptUpdateRequest
import com.weeth.domain.account.application.exception.AccountErrorCode
import com.weeth.domain.account.application.usecase.command.ManageReceiptUseCase
import com.weeth.domain.account.presentation.AccountResponseCode.RECEIPT_DELETE_SUCCESS
import com.weeth.domain.account.presentation.AccountResponseCode.RECEIPT_SAVE_SUCCESS
import com.weeth.domain.account.presentation.AccountResponseCode.RECEIPT_UPDATE_SUCCESS
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
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "RECEIPT ADMIN", description = "[ADMIN] 회비 어드민 API")
@RestController
@RequestMapping("/api/v4/admin/clubs/{clubId}/receipts")
@ApiErrorCodeExample(AccountErrorCode::class)
class ReceiptAdminController(
    private val manageReceiptUseCase: ManageReceiptUseCase,
) {
    @PostMapping
    @Operation(summary = "회비 사용 내역 기입")
    fun save(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @RequestBody @Valid dto: ReceiptSaveRequest,
    ): CommonResponse<Void> {
        manageReceiptUseCase.save(clubId, userId, dto)
        return CommonResponse.success(RECEIPT_SAVE_SUCCESS)
    }

    @DeleteMapping("/{receiptId}")
    @Operation(summary = "회비 사용 내역 취소")
    fun delete(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @PathVariable receiptId: Long,
    ): CommonResponse<Void> {
        manageReceiptUseCase.delete(clubId, userId, receiptId)
        return CommonResponse.success(RECEIPT_DELETE_SUCCESS)
    }

    @PatchMapping("/{receiptId}")
    @Operation(summary = "회비 사용 내역 수정")
    fun update(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @PathVariable receiptId: Long,
        @RequestBody @Valid dto: ReceiptUpdateRequest,
    ): CommonResponse<Void> {
        manageReceiptUseCase.update(clubId, userId, receiptId, dto)
        return CommonResponse.success(RECEIPT_UPDATE_SUCCESS)
    }
}
