package com.weeth.domain.account.presentation;

import com.weeth.domain.account.application.dto.request.AccountSaveRequest;
import com.weeth.domain.account.application.exception.AccountErrorCode;
import com.weeth.domain.account.application.usecase.command.ManageAccountUseCase;
import com.weeth.global.common.exception.ApiErrorCodeExample;
import com.weeth.global.common.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.weeth.domain.account.presentation.AccountResponseCode.ACCOUNT_SAVE_SUCCESS;

@Tag(name = "ACCOUNT ADMIN", description = "[ADMIN] 회비 어드민 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/account")
@ApiErrorCodeExample(AccountErrorCode.class)
public class AccountAdminController {

    private final ManageAccountUseCase manageAccountUseCase;

    @PostMapping
    @Operation(summary="회비 총 금액 기입")
    public CommonResponse<Void> save(@RequestBody @Valid AccountSaveRequest dto) {
        manageAccountUseCase.save(dto);
        return CommonResponse.success(ACCOUNT_SAVE_SUCCESS);
    }
}
