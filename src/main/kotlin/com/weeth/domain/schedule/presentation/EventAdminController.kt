package com.weeth.domain.schedule.presentation

import com.weeth.domain.schedule.application.dto.request.ScheduleSaveRequest
import com.weeth.domain.schedule.application.dto.request.ScheduleUpdateRequest
import com.weeth.domain.schedule.application.dto.response.EventResponse
import com.weeth.domain.schedule.application.exception.EventErrorCode
import com.weeth.domain.schedule.application.usecase.command.ManageEventUseCase
import com.weeth.domain.schedule.application.usecase.query.GetScheduleQueryService
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import com.weeth.global.common.web.TsidParam
import com.weeth.global.common.web.TsidPathVariable
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@Tag(name = "EVENT ADMIN", description = "[ADMIN] 일정 어드민 API")
@RestController
@RequestMapping("/api/v4/admin/clubs/{clubId}/events")
@ApiErrorCodeExample(EventErrorCode::class)
class EventAdminController(
    private val manageEventUseCase: ManageEventUseCase,
    private val getScheduleQueryService: GetScheduleQueryService,
) {
    @GetMapping
    @Operation(summary = "일반 일정 목록 조회")
    fun findAll(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @RequestParam(required = false) cardinal: Int?,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) start: LocalDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) end: LocalDateTime,
    ): CommonResponse<List<EventResponse>> =
        CommonResponse.success(
            ScheduleResponseCode.EVENT_FIND_ALL_SUCCESS,
            getScheduleQueryService.findEventsByAdmin(clubId, userId, cardinal, start, end),
        )

    @PostMapping
    @Operation(summary = "일정 생성")
    fun create(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @Valid @RequestBody dto: ScheduleSaveRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void?> {
        manageEventUseCase.create(clubId, dto, userId)
        return CommonResponse.success(ScheduleResponseCode.EVENT_SAVE_SUCCESS)
    }

    @PatchMapping("/{eventId}")
    @Operation(summary = "일정 수정")
    fun update(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable eventId: Long,
        @Valid @RequestBody dto: ScheduleUpdateRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void?> {
        manageEventUseCase.update(clubId, eventId, dto, userId)
        return CommonResponse.success(ScheduleResponseCode.EVENT_UPDATE_SUCCESS)
    }

    @DeleteMapping("/{eventId}")
    @Operation(summary = "일정 삭제")
    fun delete(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable eventId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void?> {
        manageEventUseCase.delete(clubId, eventId, userId)
        return CommonResponse.success(ScheduleResponseCode.EVENT_DELETE_SUCCESS)
    }
}
