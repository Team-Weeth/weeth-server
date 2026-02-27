package com.weeth.domain.schedule.presentation

import com.weeth.domain.schedule.application.dto.response.ScheduleResponse
import com.weeth.domain.schedule.application.usecase.query.GetScheduleQueryService
import com.weeth.global.common.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@Tag(name = "SCHEDULE", description = "캘린더 조회 API")
@RestController
@RequestMapping("/api/v4/schedules")
class ScheduleController(
    private val getScheduleQueryService: GetScheduleQueryService,
) {
    @GetMapping("/monthly")
    @Operation(summary = "월별 일정 조회")
    fun findByMonthly(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) start: LocalDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) end: LocalDateTime,
    ): CommonResponse<List<ScheduleResponse>> =
        CommonResponse.success(ScheduleResponseCode.SCHEDULE_MONTHLY_FIND_SUCCESS, getScheduleQueryService.findMonthly(start, end))

    @GetMapping("/yearly")
    @Operation(summary = "연도별 일정 조회")
    fun findByYearly(
        @RequestParam year: Int,
        @RequestParam semester: Int,
    ): CommonResponse<Map<Int, List<ScheduleResponse>>> =
        CommonResponse.success(ScheduleResponseCode.SCHEDULE_YEARLY_FIND_SUCCESS, getScheduleQueryService.findYearly(year, semester))
}
