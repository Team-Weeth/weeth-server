package com.weeth.domain.account.presentation

import com.weeth.domain.account.application.dto.request.ReceiptSaveRequest
import com.weeth.domain.account.application.dto.request.ReceiptUpdateRequest
import com.weeth.domain.account.application.exception.AccountErrorCode
import com.weeth.domain.account.application.usecase.command.ManageReceiptUseCase
import com.weeth.domain.account.presentation.AccountResponseCode.RECEIPT_DELETE_SUCCESS
import com.weeth.domain.account.presentation.AccountResponseCode.RECEIPT_SAVE_SUCCESS
import com.weeth.domain.account.presentation.AccountResponseCode.RECEIPT_UPDATE_SUCCESS
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
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
@RequestMapping("/api/v1/admin/receipts")
@ApiErrorCodeExample(AccountErrorCode::class)
class ReceiptAdminController(
    private val manageReceiptUseCase: ManageReceiptUseCase,
) {
    @PostMapping
    @Operation(summary = "회비 사용 내역 기입")
    fun save(
        @RequestBody @Valid dto: ReceiptSaveRequest,
    ): CommonResponse<Void> {
        manageReceiptUseCase.save(dto)
        return CommonResponse.success(RECEIPT_SAVE_SUCCESS)
    }

    @DeleteMapping("/{receiptId}")
    @Operation(summary = "회비 사용 내역 취소")
    fun delete(
        @PathVariable receiptId: Long,
    ): CommonResponse<Void> {
        manageReceiptUseCase.delete(receiptId)
        return CommonResponse.success(RECEIPT_DELETE_SUCCESS)
    }

    @PatchMapping("/{receiptId}")
    @Operation(summary = "회비 사용 내역 수정")
    fun update(
        @PathVariable receiptId: Long,
        @RequestBody @Valid dto: ReceiptUpdateRequest,
    ): CommonResponse<Void> {
        manageReceiptUseCase.update(receiptId, dto)
        return CommonResponse.success(RECEIPT_UPDATE_SUCCESS)
    }
}
