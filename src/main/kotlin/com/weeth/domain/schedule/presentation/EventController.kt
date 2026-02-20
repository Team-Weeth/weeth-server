package com.weeth.domain.schedule.presentation

import com.weeth.domain.schedule.application.dto.response.EventResponse
import com.weeth.domain.schedule.application.exception.EventErrorCode
import com.weeth.domain.schedule.application.usecase.command.ManageEventUseCase
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "EVENT", description = "일정 API")
@RestController
@RequestMapping("/api/v4/events")
@ApiErrorCodeExample(EventErrorCode::class)
class EventController(
    private val manageEventUseCase: ManageEventUseCase,
) {
    @GetMapping("/{eventId}")
    @Operation(summary = "일정 상세 조회")
    fun find(
        @PathVariable eventId: Long,
    ): CommonResponse<EventResponse> = CommonResponse.success(ScheduleResponseCode.EVENT_FIND_SUCCESS, manageEventUseCase.find(eventId))
}
