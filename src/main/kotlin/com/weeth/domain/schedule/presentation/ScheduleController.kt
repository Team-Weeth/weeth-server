package com.weeth.domain.schedule.presentation

import com.weeth.domain.schedule.application.dto.response.ScheduleResponse
import com.weeth.domain.schedule.application.usecase.query.GetScheduleQueryService
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.common.response.CommonResponse
import com.weeth.global.common.web.TsidParam
import com.weeth.global.common.web.TsidPathVariable
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@Tag(name = "SCHEDULE", description = "캘린더 조회 API")
@RestController
@RequestMapping("/api/v4/clubs/{clubId}/schedules")
class ScheduleController(
    private val getScheduleQueryService: GetScheduleQueryService,
) {
    @GetMapping("/monthly")
    @Operation(summary = "월별 일정 조회")
    fun findByMonthly(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) start: LocalDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) end: LocalDateTime,
    ): CommonResponse<List<ScheduleResponse>> =
        CommonResponse.success(
            ScheduleResponseCode.SCHEDULE_MONTHLY_FIND_SUCCESS,
            getScheduleQueryService.findMonthly(clubId, userId, start, end),
        )

    @GetMapping("/yearly")
    @Operation(summary = "연도별 일정 조회")
    fun findByYearly(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @RequestParam year: Int,
    ): CommonResponse<Map<Int, List<ScheduleResponse>>> =
        CommonResponse.success(
            ScheduleResponseCode.SCHEDULE_YEARLY_FIND_SUCCESS,
            getScheduleQueryService.findYearly(clubId, userId, year),
        )
}
