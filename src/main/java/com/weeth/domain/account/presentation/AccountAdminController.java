package com.weeth.domain.account.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.weeth.domain.account.application.dto.AccountDTO;
import com.weeth.domain.account.application.exception.AccountErrorCode;
import com.weeth.domain.account.application.usecase.AccountUseCase;
import com.weeth.global.common.exception.ApiErrorCodeExample;
import com.weeth.global.common.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.weeth.domain.account.presentation.ResponseMessage.ACCOUNT_SAVE_SUCCESS;

@Tag(name = "ACCOUNT ADMIN", description = "[ADMIN] 회비 어드민 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/account")
@ApiErrorCodeExample(AccountErrorCode.class)
public class AccountAdminController {

    private final AccountUseCase accountUseCase;

    @PostMapping
    @Operation(summary="회비 총 금액 기입")
    public CommonResponse<Void> save(@RequestBody @Valid AccountDTO.Save dto) {
        accountUseCase.save(dto);
        return CommonResponse.createSuccess(ACCOUNT_SAVE_SUCCESS.getMessage());
    }
}
