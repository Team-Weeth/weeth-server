package com.weeth.domain.penalty.presentation

import com.weeth.domain.penalty.application.dto.response.PenaltyResponse
import com.weeth.domain.penalty.application.exception.PenaltyErrorCode
import com.weeth.domain.penalty.application.usecase.query.GetPenaltyQueryService
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import com.weeth.global.common.web.TsidParam
import com.weeth.global.common.web.TsidPathVariable
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "PENALTY", description = "패널티 API")
@RestController
@RequestMapping("/api/v4/clubs/{clubId}/penalties")
@ApiErrorCodeExample(PenaltyErrorCode::class)
class PenaltyUserController(
    private val getPenaltyQueryService: GetPenaltyQueryService,
) {
    @GetMapping
    @Operation(summary = "본인 패널티 조회", hidden = true)
    fun findAllPenalties(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<PenaltyResponse> =
        CommonResponse.success(
            PenaltyResponseCode.PENALTY_USER_FIND_SUCCESS,
            getPenaltyQueryService.findByUser(clubId, userId),
        )
}
