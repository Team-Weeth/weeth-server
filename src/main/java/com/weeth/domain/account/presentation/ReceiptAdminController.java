package com.weeth.domain.account.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.weeth.domain.account.application.dto.ReceiptDTO;
import com.weeth.domain.account.application.exception.AccountErrorCode;
import com.weeth.domain.account.application.usecase.ReceiptUseCase;
import com.weeth.global.common.exception.ApiErrorCodeExample;
import com.weeth.global.common.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import static com.weeth.domain.account.presentation.ResponseMessage.*;

@Tag(name = "RECEIPT ADMIN", description = "[ADMIN] 회비 어드민 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/receipts")
@ApiErrorCodeExample(AccountErrorCode.class)
public class ReceiptAdminController {

    private final ReceiptUseCase receiptUseCase;

    @PostMapping
    @Operation(summary="회비 사용 내역 기입")
    public CommonResponse<Void> save(@RequestBody @Valid ReceiptDTO.Save dto) {
        receiptUseCase.save(dto);
        return CommonResponse.createSuccess(RECEIPT_SAVE_SUCCESS.getMessage());
    }

    @DeleteMapping("/{receiptId}")
    @Operation(summary="회비 사용 내역 취소")
    public CommonResponse<Void> delete(@PathVariable Long receiptId) {
        receiptUseCase.delete(receiptId);
        return CommonResponse.createSuccess(RECEIPT_DELETE_SUCCESS.getMessage());
    }

    @PatchMapping("/{receiptId}")
    @Operation(summary="회비 사용 내역 수정")
    public CommonResponse<Void> update(@PathVariable Long receiptId, @RequestBody @Valid ReceiptDTO.Update dto) {
        receiptUseCase.update(receiptId, dto);
        return CommonResponse.createSuccess(RECEIPT_UPDATE_SUCCESS.getMessage());
    }
}
