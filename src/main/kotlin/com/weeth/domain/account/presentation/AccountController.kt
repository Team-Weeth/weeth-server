package com.weeth.domain.account.presentation

import com.weeth.domain.account.application.dto.response.AccountResponse
import com.weeth.domain.account.application.exception.AccountErrorCode
import com.weeth.domain.account.application.usecase.query.GetAccountQueryService
import com.weeth.domain.account.presentation.AccountResponseCode.ACCOUNT_FIND_SUCCESS
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "ACCOUNT", description = "회비 API")
@RestController
@RequestMapping("/api/v1/account")
@ApiErrorCodeExample(AccountErrorCode::class)
class AccountController(
    private val getAccountQueryService: GetAccountQueryService,
) {
    @GetMapping("/{cardinal}")
    @Operation(summary = "회비 내역 조회")
    fun find(
        @PathVariable cardinal: Int,
    ): CommonResponse<AccountResponse> = CommonResponse.success(ACCOUNT_FIND_SUCCESS, getAccountQueryService.findByCardinal(cardinal))
}
