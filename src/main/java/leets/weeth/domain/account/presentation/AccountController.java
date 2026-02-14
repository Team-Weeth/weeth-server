package leets.weeth.domain.account.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import leets.weeth.domain.account.application.dto.AccountDTO;
import leets.weeth.domain.account.application.exception.AccountErrorCode;
import leets.weeth.domain.account.application.usecase.AccountUseCase;
import leets.weeth.global.common.exception.ApiErrorCodeExample;
import leets.weeth.global.common.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static leets.weeth.domain.account.presentation.ResponseMessage.ACCOUNT_FIND_SUCCESS;
@Tag(name = "ACCOUNT", description = "회비 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/account")
@ApiErrorCodeExample(AccountErrorCode.class)
public class AccountController {

    private final AccountUseCase accountUseCase;

    @GetMapping("/{cardinal}")
    @Operation(summary="회비 내역 조회")
    public CommonResponse<AccountDTO.Response> find(@PathVariable Integer cardinal) {
        return CommonResponse.createSuccess(ACCOUNT_FIND_SUCCESS.getMessage(),accountUseCase.find(cardinal));
    }
}
