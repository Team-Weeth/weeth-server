package com.weeth.domain.penalty.presentation

import com.weeth.domain.penalty.application.dto.request.SavePenaltyRequest
import com.weeth.domain.penalty.application.dto.request.UpdatePenaltyRequest
import com.weeth.domain.penalty.application.dto.response.PenaltyByCardinalResponse
import com.weeth.domain.penalty.application.exception.PenaltyErrorCode
import com.weeth.domain.penalty.application.usecase.command.DeletePenaltyUseCase
import com.weeth.domain.penalty.application.usecase.command.SavePenaltyUseCase
import com.weeth.domain.penalty.application.usecase.command.UpdatePenaltyUseCase
import com.weeth.domain.penalty.application.usecase.query.GetPenaltyQueryService
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "PENALTY ADMIN", description = "[ADMIN] 패널티 어드민 API")
@RestController
@RequestMapping("/api/v1/admin/penalties")
@ApiErrorCodeExample(PenaltyErrorCode::class)
class PenaltyAdminController(
    private val savePenaltyUseCase: SavePenaltyUseCase,
    private val updatePenaltyUseCase: UpdatePenaltyUseCase,
    private val deletePenaltyUseCase: DeletePenaltyUseCase,
    private val getPenaltyQueryService: GetPenaltyQueryService,
) {
    @PostMapping
    @Operation(summary = "패널티 부여")
    fun assignPenalty(
        @Valid @RequestBody request: SavePenaltyRequest,
    ): CommonResponse<Void?> {
        savePenaltyUseCase.execute(request)
        return CommonResponse.success(PenaltyResponseCode.PENALTY_ASSIGN_SUCCESS)
    }

    @PatchMapping
    @Operation(summary = "패널티 수정")
    fun update(
        @Valid @RequestBody request: UpdatePenaltyRequest,
    ): CommonResponse<Void?> {
        updatePenaltyUseCase.execute(request)
        return CommonResponse.success(PenaltyResponseCode.PENALTY_UPDATE_SUCCESS)
    }

    @GetMapping
    @Operation(summary = "전체 패널티 조회")
    fun findAll(
        @RequestParam(required = false) cardinal: Int?,
    ): CommonResponse<List<PenaltyByCardinalResponse>> =
        CommonResponse.success(PenaltyResponseCode.PENALTY_FIND_ALL_SUCCESS, getPenaltyQueryService.findAll(cardinal))

    @DeleteMapping
    @Operation(summary = "패널티 삭제")
    fun delete(
        @RequestParam penaltyId: Long,
    ): CommonResponse<Void?> {
        deletePenaltyUseCase.execute(penaltyId)
        return CommonResponse.success(PenaltyResponseCode.PENALTY_DELETE_SUCCESS)
    }
}
