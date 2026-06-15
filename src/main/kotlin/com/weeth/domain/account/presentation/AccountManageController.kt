package com.weeth.domain.account.presentation

import com.weeth.domain.account.application.dto.request.UpdateMemberVisibilityRequest
import com.weeth.domain.account.application.usecase.command.ManageAccountUseCase
import com.weeth.domain.account.presentation.AccountResponseCode.ACCOUNT_UPDATE_SUCCESS
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.common.response.CommonResponse
import com.weeth.global.common.web.TsidParam
import com.weeth.global.common.web.TsidPathVariable
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "ACCOUNT ADMIN", description = "[ADMIN] 회비 어드민 API")
@RestController
@RequestMapping("/api/v4/admin/clubs/{clubId}/accounts")
class AccountManageController(
    private val manageAccountUseCase: ManageAccountUseCase,
) {
    @PatchMapping("/{accountId}/member-visibility")
    @Operation(summary = "부원 거래 내역 공개 여부 수정")
    fun updateMemberVisibility(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable accountId: Long,
        @RequestBody @Valid request: UpdateMemberVisibilityRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void> {
        manageAccountUseCase.updateMemberVisibility(
            clubId = clubId,
            accountId = accountId,
            visible = request.visible,
            userId = userId,
        )
        return CommonResponse.success(ACCOUNT_UPDATE_SUCCESS)
    }
}
