package com.weeth.domain.attendance.presentation

import com.weeth.domain.attendance.application.dto.request.CheckInRequest
import com.weeth.domain.attendance.application.dto.response.AttendanceDetailResponse
import com.weeth.domain.attendance.application.dto.response.AttendanceSummaryResponse
import com.weeth.domain.attendance.application.exception.AttendanceErrorCode
import com.weeth.domain.attendance.application.usecase.command.ManageAttendanceUseCase
import com.weeth.domain.attendance.application.usecase.query.GetAttendanceQueryService
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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "ATTENDANCE", description = "출석 API")
@RestController
@RequestMapping("/api/v4/clubs/{clubId}/attendances")
@ApiErrorCodeExample(AttendanceErrorCode::class)
class AttendanceController(
    private val manageAttendanceUseCase: ManageAttendanceUseCase,
    private val getAttendanceQueryService: GetAttendanceQueryService,
) {
    @PostMapping("/check-in")
    @Operation(summary = "출석체크")
    fun checkIn(
        @PathVariable @TsidParam
        @TsidPathVariable clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @RequestBody checkIn: CheckInRequest,
    ): CommonResponse<Void?> {
        manageAttendanceUseCase.checkIn(clubId, userId, checkIn.sessionId, checkIn.code)
        return CommonResponse.success(AttendanceResponseCode.ATTENDANCE_CHECKIN_SUCCESS)
    }

    @GetMapping
    @Operation(summary = "출석 메인페이지")
    fun find(
        @PathVariable @TsidParam
        @TsidPathVariable clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<AttendanceSummaryResponse> =
        CommonResponse.success(
            AttendanceResponseCode.ATTENDANCE_FIND_SUCCESS,
            getAttendanceQueryService.findAttendance(clubId, userId),
        )

    @GetMapping("/detail")
    @Operation(summary = "출석 내역 상세조회")
    fun findAll(
        @PathVariable @TsidParam
        @TsidPathVariable clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<AttendanceDetailResponse> =
        CommonResponse.success(
            AttendanceResponseCode.ATTENDANCE_FIND_ALL_SUCCESS,
            getAttendanceQueryService.findAllDetailsByCurrentCardinal(clubId, userId),
        )
}
