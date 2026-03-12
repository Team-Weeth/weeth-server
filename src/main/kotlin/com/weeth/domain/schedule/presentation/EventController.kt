package com.weeth.domain.schedule.presentation

import com.weeth.domain.schedule.application.dto.response.EventResponse
import com.weeth.domain.schedule.application.exception.EventErrorCode
import com.weeth.domain.schedule.application.usecase.query.GetScheduleQueryService
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import com.weeth.global.common.web.TsidParam
import com.weeth.global.common.web.TsidPathVariable
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "EVENT", description = "일정 API")
@RestController
@RequestMapping("/api/v4/clubs/{clubId}/events")
@ApiErrorCodeExample(EventErrorCode::class)
class EventController(
    private val getScheduleQueryService: GetScheduleQueryService,
) {
    @GetMapping("/{eventId}")
    @Operation(summary = "일정 상세 조회")
    fun getEvent(
        @PathVariable @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable eventId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<EventResponse> =
        CommonResponse.success(
            ScheduleResponseCode.EVENT_FIND_SUCCESS,
            getScheduleQueryService.findEvent(clubId, userId, eventId),
        )
}
