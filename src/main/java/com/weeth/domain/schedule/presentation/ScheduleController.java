package com.weeth.domain.schedule.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.weeth.domain.schedule.application.dto.response.ScheduleResponse;
import com.weeth.domain.schedule.application.exception.EventErrorCode;
import com.weeth.domain.schedule.application.exception.MeetingErrorCode;
import com.weeth.domain.schedule.application.usecase.query.GetScheduleQueryService;
import com.weeth.global.common.exception.ApiErrorCodeExample;
import com.weeth.global.common.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.weeth.domain.schedule.presentation.ScheduleResponseCode.SCHEDULE_MONTHLY_FIND_SUCCESS;
import static com.weeth.domain.schedule.presentation.ScheduleResponseCode.SCHEDULE_YEARLY_FIND_SUCCESS;

@Tag(name = "SCHEDULE", description = "캘린더 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/schedules")
@ApiErrorCodeExample({EventErrorCode.class, MeetingErrorCode.class})
public class ScheduleController {

    private final GetScheduleQueryService getScheduleQueryService;

    @GetMapping("/monthly")
    @Operation(summary="월별 일정 조회")
    public CommonResponse<List<ScheduleResponse>> findByMonthly(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return CommonResponse.success(SCHEDULE_MONTHLY_FIND_SUCCESS, getScheduleQueryService.findMonthly(start, end));
    }

    @GetMapping("/yearly")
    @Operation(summary="연도별 일정 조회")
    public CommonResponse<Map<Integer, List<ScheduleResponse>>> findByYearly(
            @RequestParam Integer year,
            @RequestParam Integer semester) {
        return CommonResponse.success(SCHEDULE_YEARLY_FIND_SUCCESS, getScheduleQueryService.findYearly(year, semester));
    }
}
