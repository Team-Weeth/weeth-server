package com.weeth.domain.cardinal.presentation

import com.weeth.domain.cardinal.application.dto.request.CardinalSaveRequest
import com.weeth.domain.cardinal.application.exception.CardinalErrorCode
import com.weeth.domain.cardinal.application.usecase.command.ManageCardinalUseCase
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.auth.jwt.application.exception.JwtErrorCode
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import com.weeth.global.common.web.TsidParam
import com.weeth.global.common.web.TsidPathVariable
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "CARDINAL ADMIN", description = "[ADMIN] 기수 어드민 API")
@RestController
@RequestMapping("/api/v4/admin/clubs/{clubId}/cardinals")
@ApiErrorCodeExample(CardinalErrorCode::class, JwtErrorCode::class)
class CardinalAdminController(
    private val manageCardinalUseCase: ManageCardinalUseCase,
) {
    @PatchMapping("/{cardinalId}")
    @Operation(summary = "현재 진행 기수 지정 API")
    fun activate(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable cardinalId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void> {
        manageCardinalUseCase.activate(clubId, cardinalId, userId)
        return CommonResponse.success(CardinalResponseCode.CARDINAL_UPDATE_SUCCESS)
    }

    @PostMapping
    @Operation(summary = "새로운 기수 정보 저장 API")
    fun save(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @RequestBody @Valid request: CardinalSaveRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void> {
        manageCardinalUseCase.save(clubId, request, userId)
        return CommonResponse.success(CardinalResponseCode.CARDINAL_SAVE_SUCCESS)
    }
}
