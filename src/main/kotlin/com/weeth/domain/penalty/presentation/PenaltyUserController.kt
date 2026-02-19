package com.weeth.domain.penalty.presentation

import com.weeth.domain.penalty.application.dto.response.PenaltyResponse
import com.weeth.domain.penalty.application.exception.PenaltyErrorCode
import com.weeth.domain.penalty.application.usecase.query.GetPenaltyQueryService
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "PENALTY", description = "패널티 API")
@RestController
@RequestMapping("/api/v1/penalties")
@ApiErrorCodeExample(PenaltyErrorCode::class)
class PenaltyUserController(
    private val getPenaltyQueryService: GetPenaltyQueryService,
) {
    @GetMapping
    @Operation(summary = "본인 패널티 조회")
    fun findAllPenalties(
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<PenaltyResponse> =
        CommonResponse.success(PenaltyResponseCode.PENALTY_USER_FIND_SUCCESS, getPenaltyQueryService.findByUser(userId))
}
