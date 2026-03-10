package com.weeth.domain.cardinal.presentation

import com.weeth.domain.cardinal.application.dto.response.CardinalResponse
import com.weeth.domain.cardinal.application.exception.CardinalErrorCode
import com.weeth.domain.cardinal.application.usecase.query.GetCardinalQueryService
import com.weeth.global.auth.jwt.application.exception.JwtErrorCode
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import com.weeth.global.common.web.TsidParam
import com.weeth.global.common.web.TsidPathVariable
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "CARDINAL")
@RestController
@RequestMapping("/api/v4/clubs/{clubId}/cardinals")
@ApiErrorCodeExample(CardinalErrorCode::class, JwtErrorCode::class)
class CardinalController(
    private val getCardinalQueryService: GetCardinalQueryService,
) {
    @GetMapping
    @Operation(summary = "현재 저장된 기수 목록 조회 API")
    fun findAllCardinals(
        @PathVariable @TsidParam
        @TsidPathVariable clubId: Long,
    ): CommonResponse<List<CardinalResponse>> =
        CommonResponse.success(CardinalResponseCode.CARDINAL_FIND_ALL_SUCCESS, getCardinalQueryService.findAll(clubId))
}
