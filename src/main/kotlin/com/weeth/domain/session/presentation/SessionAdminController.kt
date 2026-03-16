package com.weeth.domain.session.presentation

import com.weeth.domain.schedule.application.dto.request.ScheduleSaveRequest
import com.weeth.domain.schedule.application.dto.request.ScheduleUpdateRequest
import com.weeth.domain.schedule.application.dto.response.SessionInfosResponse
import com.weeth.domain.session.application.exception.SessionErrorCode
import com.weeth.domain.session.application.usecase.command.ManageSessionUseCase
import com.weeth.domain.session.application.usecase.query.GetSessionQueryService
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
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "SESSION ADMIN", description = "[ADMIN] 정기모임 어드민 API")
@RestController
@RequestMapping("/api/v4/admin/clubs/{clubId}/sessions")
@ApiErrorCodeExample(SessionErrorCode::class)
class SessionAdminController(
    private val manageSessionUseCase: ManageSessionUseCase,
    private val getSessionQueryService: GetSessionQueryService,
) {
    @PostMapping
    @Operation(summary = "정기모임 생성")
    fun create(
        @PathVariable @TsidParam
        @TsidPathVariable clubId: Long,
        @Valid @RequestBody dto: ScheduleSaveRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void?> {
        manageSessionUseCase.create(clubId, dto, userId)
        return CommonResponse.success(SessionResponseCode.SESSION_SAVE_SUCCESS)
    }

    @PatchMapping("/{sessionId}")
    @Operation(summary = "정기모임 수정")
    fun update(
        @PathVariable @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable sessionId: Long,
        @Valid @RequestBody dto: ScheduleUpdateRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void?> {
        manageSessionUseCase.update(clubId, sessionId, dto, userId)
        return CommonResponse.success(SessionResponseCode.SESSION_UPDATE_SUCCESS)
    }

    @DeleteMapping("/{sessionId}")
    @Operation(summary = "정기모임 삭제")
    fun delete(
        @PathVariable @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable sessionId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void?> {
        manageSessionUseCase.delete(clubId, sessionId, userId)
        return CommonResponse.success(SessionResponseCode.SESSION_DELETE_SUCCESS)
    }

    @GetMapping
    @Operation(summary = "정기모임 목록 조회")
    fun getSessionInfos(
        @PathVariable @TsidParam
        @TsidPathVariable clubId: Long,
        @RequestParam(required = false) cardinal: Int?,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<SessionInfosResponse> =
        CommonResponse.success(
            SessionResponseCode.SESSION_INFOS_FIND_SUCCESS,
            getSessionQueryService.findSessionInfos(clubId, userId, cardinal),
        )
}
