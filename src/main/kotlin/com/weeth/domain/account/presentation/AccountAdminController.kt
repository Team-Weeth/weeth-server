package com.weeth.domain.account.presentation

import com.weeth.domain.account.application.dto.request.AccountSaveRequest
import com.weeth.domain.account.application.exception.AccountErrorCode
import com.weeth.domain.account.application.usecase.command.ManageAccountUseCase
import com.weeth.domain.account.presentation.AccountResponseCode.ACCOUNT_SAVE_SUCCESS
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "ACCOUNT ADMIN", description = "[ADMIN] 회비 어드민 API")
@RestController
@RequestMapping("/api/v1/admin/account")
@ApiErrorCodeExample(AccountErrorCode::class)
class AccountAdminController(
    private val manageAccountUseCase: ManageAccountUseCase,
) {
    @PostMapping
    @Operation(summary = "회비 총 금액 기입")
    fun save(
        @RequestBody @Valid dto: AccountSaveRequest,
    ): CommonResponse<Void> {
        manageAccountUseCase.save(dto)
        return CommonResponse.success(ACCOUNT_SAVE_SUCCESS)
    }
}
