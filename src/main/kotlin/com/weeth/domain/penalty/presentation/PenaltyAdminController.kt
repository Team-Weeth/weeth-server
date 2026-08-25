package com.weeth.domain.penalty.presentation

import com.weeth.domain.penalty.application.dto.request.SavePenaltyRequest
import com.weeth.domain.penalty.application.dto.request.SavePenaltyRuleRequest
import com.weeth.domain.penalty.application.dto.request.UpdatePenaltyRequest
import com.weeth.domain.penalty.application.dto.response.MemberPenaltyDetailResponse
import com.weeth.domain.penalty.application.dto.response.PenaltyByCardinalResponse
import com.weeth.domain.penalty.application.exception.PenaltyErrorCode
import com.weeth.domain.penalty.application.usecase.command.DeletePenaltyUseCase
import com.weeth.domain.penalty.application.usecase.command.SavePenaltyRuleUseCase
import com.weeth.domain.penalty.application.usecase.command.SavePenaltyUseCase
import com.weeth.domain.penalty.application.usecase.command.UpdatePenaltyUseCase
import com.weeth.domain.penalty.application.usecase.query.GetPenaltyQueryService
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import com.weeth.global.common.web.TsidParam
import com.weeth.global.common.web.TsidPathVariable
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "PENALTY ADMIN", description = "[ADMIN] 페널티 어드민 API")
@RestController
@RequestMapping("/api/v4/admin/clubs/{clubId}/penalties")
@ApiErrorCodeExample(PenaltyErrorCode::class)
class PenaltyAdminController(
    private val savePenaltyUseCase: SavePenaltyUseCase,
    private val savePenaltyRuleUseCase: SavePenaltyRuleUseCase,
    private val updatePenaltyUseCase: UpdatePenaltyUseCase,
    private val deletePenaltyUseCase: DeletePenaltyUseCase,
    private val getPenaltyQueryService: GetPenaltyQueryService,
) {
    @PostMapping
    @Operation(summary = "페널티 부여")
    fun assignPenalty(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @Valid @RequestBody request: SavePenaltyRequest,
    ): CommonResponse<Void?> {
        savePenaltyUseCase.save(clubId, userId, request)
        return CommonResponse.success(PenaltyResponseCode.PENALTY_ASSIGN_SUCCESS)
    }

    @PatchMapping
    @Operation(summary = "페널티 수정")
    fun update(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @Valid @RequestBody request: UpdatePenaltyRequest,
    ): CommonResponse<Void?> {
        updatePenaltyUseCase.update(clubId, userId, request)
        return CommonResponse.success(PenaltyResponseCode.PENALTY_UPDATE_SUCCESS)
    }

    @GetMapping
    @Operation(summary = "전체 페널티 조회", hidden = true)
    fun findAll(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @RequestParam(required = false) cardinal: Int?,
    ): CommonResponse<List<PenaltyByCardinalResponse>> =
        CommonResponse.success(
            PenaltyResponseCode.PENALTY_FIND_ALL_SUCCESS,
            getPenaltyQueryService.findAllByCardinal(clubId, userId, cardinal),
        )

    @GetMapping("/members/{clubMemberId}")
    @Operation(summary = "멤버 페널티 상세 조회")
    fun findMemberPenaltyDetail(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @PathVariable clubMemberId: Long,
    ): CommonResponse<MemberPenaltyDetailResponse> =
        CommonResponse.success(
            PenaltyResponseCode.PENALTY_MEMBER_DETAIL_SUCCESS,
            getPenaltyQueryService.findMemberPenaltyDetail(clubId, userId, clubMemberId),
        )

    @PutMapping("/rule")
    @Operation(summary = "패널티 규정 저장")
    fun saveRule(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @Valid @RequestBody request: SavePenaltyRuleRequest,
    ): CommonResponse<Void?> {
        savePenaltyRuleUseCase.save(clubId, userId, request)
        return CommonResponse.success(PenaltyResponseCode.PENALTY_RULE_SAVE_SUCCESS)
    }

    @DeleteMapping
    @Operation(summary = "페널티 삭제")
    fun delete(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @RequestParam penaltyId: Long,
    ): CommonResponse<Void?> {
        deletePenaltyUseCase.delete(clubId, userId, penaltyId)
        return CommonResponse.success(PenaltyResponseCode.PENALTY_DELETE_SUCCESS)
    }
}
