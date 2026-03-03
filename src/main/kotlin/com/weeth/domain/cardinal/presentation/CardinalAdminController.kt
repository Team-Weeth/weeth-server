package com.weeth.domain.cardinal.presentation

import com.weeth.domain.cardinal.application.dto.request.CardinalSaveRequest
import com.weeth.domain.cardinal.application.dto.request.CardinalUpdateRequest
import com.weeth.domain.cardinal.application.exception.CardinalErrorCode
import com.weeth.domain.cardinal.application.usecase.command.ManageCardinalUseCase
import com.weeth.global.auth.jwt.application.exception.JwtErrorCode
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "CARDINAL ADMIN", description = "[ADMIN] 기수 어드민 API")
@RestController
@RequestMapping("/api/v4/admin/cardinals")
@ApiErrorCodeExample(CardinalErrorCode::class, JwtErrorCode::class)
class CardinalAdminController(
    private val manageCardinalUseCase: ManageCardinalUseCase,
) {
    @PatchMapping
    @Operation(summary = "기수 정보 수정 API")
    fun update(
        @RequestBody @Valid request: CardinalUpdateRequest,
    ): CommonResponse<Void> {
        manageCardinalUseCase.update(request)
        return CommonResponse.success(CardinalResponseCode.CARDINAL_UPDATE_SUCCESS)
    }

    @PostMapping
    @Operation(summary = "새로운 기수 정보 저장 API")
    fun save(
        @RequestBody @Valid request: CardinalSaveRequest,
    ): CommonResponse<Void> {
        manageCardinalUseCase.save(request)
        return CommonResponse.success(CardinalResponseCode.CARDINAL_SAVE_SUCCESS)
    }
}
