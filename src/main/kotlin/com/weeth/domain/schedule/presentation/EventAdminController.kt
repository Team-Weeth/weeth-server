package com.weeth.domain.schedule.presentation

import com.weeth.domain.schedule.application.dto.request.ScheduleSaveRequest
import com.weeth.domain.schedule.application.dto.request.ScheduleUpdateRequest
import com.weeth.domain.schedule.application.exception.EventErrorCode
import com.weeth.domain.schedule.application.usecase.command.ManageEventUseCase
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "EVENT ADMIN", description = "[ADMIN] 일정 어드민 API")
@RestController
@RequestMapping("/api/v4/admin/events")
@ApiErrorCodeExample(EventErrorCode::class)
class EventAdminController(
    private val manageEventUseCase: ManageEventUseCase,
) {
    @PostMapping
    @Operation(summary = "일정 생성")
    fun create(
        @Valid @RequestBody dto: ScheduleSaveRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void?> {
        manageEventUseCase.create(dto, userId)
        return CommonResponse.success(ScheduleResponseCode.EVENT_SAVE_SUCCESS)
    }

    @PatchMapping("/{eventId}")
    @Operation(summary = "일정 수정")
    fun update(
        @PathVariable eventId: Long,
        @Valid @RequestBody dto: ScheduleUpdateRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void?> {
        manageEventUseCase.update(eventId, dto, userId)
        return CommonResponse.success(ScheduleResponseCode.EVENT_UPDATE_SUCCESS)
    }

    @DeleteMapping("/{eventId}")
    @Operation(summary = "일정 삭제")
    fun delete(
        @PathVariable eventId: Long,
    ): CommonResponse<Void?> {
        manageEventUseCase.delete(eventId)
        return CommonResponse.success(ScheduleResponseCode.EVENT_DELETE_SUCCESS)
    }
}
