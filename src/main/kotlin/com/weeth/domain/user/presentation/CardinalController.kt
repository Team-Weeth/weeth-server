package com.weeth.domain.user.presentation

import com.weeth.domain.user.application.dto.request.CardinalSaveRequest
import com.weeth.domain.user.application.dto.request.CardinalUpdateRequest
import com.weeth.domain.user.application.dto.response.CardinalResponse
import com.weeth.domain.user.application.exception.UserErrorCode
import com.weeth.domain.user.application.usecase.command.ManageCardinalUseCase
import com.weeth.domain.user.application.usecase.query.GetCardinalQueryService
import com.weeth.global.auth.jwt.application.exception.JwtErrorCode
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "CARDINAL")
@RestController
@RequestMapping("/api/v4")
@ApiErrorCodeExample(UserErrorCode::class, JwtErrorCode::class)
class CardinalController(
    private val manageCardinalUseCase: ManageCardinalUseCase,
    private val getCardinalQueryService: GetCardinalQueryService,
) {
    @GetMapping("/cardinals")
    @Operation(summary = "현재 저장된 기수 목록 조회 API")
    fun findAllCardinals(): CommonResponse<List<CardinalResponse>> =
        CommonResponse.success(UserResponseCode.CARDINAL_FIND_ALL_SUCCESS, getCardinalQueryService.findAll())

    @PatchMapping("/admin/cardinals") // todo: 어드민 컨트롤러 분리
    @Operation(summary = "[admin] 기수 정보 수정 API")
    fun updateCardinals(
        @RequestBody @Valid request: CardinalUpdateRequest,
    ): CommonResponse<Void> {
        manageCardinalUseCase.update(request)
        return CommonResponse.success(UserResponseCode.CARDINAL_UPDATE_SUCCESS)
    }

    @PostMapping("/admin/cardinals")
    @Operation(summary = "[admin] 새로운 기수 정보 저장 API")
    fun save(
        @RequestBody @Valid request: CardinalSaveRequest,
    ): CommonResponse<Void> {
        manageCardinalUseCase.save(request)
        return CommonResponse.success(UserResponseCode.CARDINAL_SAVE_SUCCESS)
    }
}
