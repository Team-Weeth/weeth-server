package com.weeth.domain.attendance.presentation

import com.weeth.domain.attendance.application.usecase.command.ManageSessionUseCase
import com.weeth.domain.schedule.application.dto.request.ScheduleSaveRequest
import com.weeth.domain.schedule.application.dto.request.ScheduleUpdateRequest
import com.weeth.domain.schedule.application.dto.response.SessionInfosResponse
import com.weeth.domain.schedule.application.exception.MeetingErrorCode
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
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
@RequestMapping("/api/v4/admin/sessions")
@ApiErrorCodeExample(MeetingErrorCode::class)
class SessionAdminController(
    private val manageSessionUseCase: ManageSessionUseCase,
) {
    @PostMapping
    @Operation(summary = "정기모임 생성")
    fun create(
        @Valid @RequestBody dto: ScheduleSaveRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void?> {
        manageSessionUseCase.create(dto, userId)
        return CommonResponse.success(AttendanceResponseCode.SESSION_SAVE_SUCCESS)
    }

    @PatchMapping("/{sessionId}")
    @Operation(summary = "정기모임 수정")
    fun update(
        @PathVariable sessionId: Long,
        @Valid @RequestBody dto: ScheduleUpdateRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void?> {
        manageSessionUseCase.update(sessionId, dto, userId)
        return CommonResponse.success(AttendanceResponseCode.SESSION_UPDATE_SUCCESS)
    }

    @DeleteMapping("/{sessionId}")
    @Operation(summary = "정기모임 삭제")
    fun delete(
        @PathVariable sessionId: Long,
    ): CommonResponse<Void?> {
        manageSessionUseCase.delete(sessionId)
        return CommonResponse.success(AttendanceResponseCode.SESSION_DELETE_SUCCESS)
    }

    @GetMapping
    @Operation(summary = "정기모임 목록 조회")
    fun findInfos(
        @RequestParam(required = false) cardinal: Int?,
    ): CommonResponse<SessionInfosResponse> =
        CommonResponse.success(AttendanceResponseCode.MEETING_FIND_SUCCESS, manageSessionUseCase.findInfos(cardinal))
}
