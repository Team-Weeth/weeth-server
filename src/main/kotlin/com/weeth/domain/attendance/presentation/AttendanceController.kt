package com.weeth.domain.attendance.presentation

import com.weeth.domain.attendance.application.dto.request.CheckInRequest
import com.weeth.domain.attendance.application.dto.response.AttendanceDetailResponse
import com.weeth.domain.attendance.application.dto.response.AttendanceMainResponse
import com.weeth.domain.attendance.application.exception.AttendanceErrorCode
import com.weeth.domain.attendance.application.usecase.AttendanceUseCase
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "ATTENDANCE", description = "출석 API")
@RestController
@RequestMapping("/api/v1/attendances")
@ApiErrorCodeExample(AttendanceErrorCode::class)
class AttendanceController(
    private val attendanceUseCase: AttendanceUseCase,
) {
    @PatchMapping
    @Operation(summary = "출석체크")
    fun checkIn(
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @RequestBody checkIn: CheckInRequest,
    ): CommonResponse<Void?> {
        attendanceUseCase.checkIn(userId, checkIn.code)
        return CommonResponse.success(AttendanceResponseCode.ATTENDANCE_CHECKIN_SUCCESS)
    }

    @GetMapping
    @Operation(summary = "출석 메인페이지")
    fun find(
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<AttendanceMainResponse> =
        CommonResponse.success(AttendanceResponseCode.ATTENDANCE_FIND_SUCCESS, attendanceUseCase.find(userId))

    @GetMapping("/detail")
    @Operation(summary = "출석 내역 상세조회")
    fun findAll(
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<AttendanceDetailResponse> =
        CommonResponse.success(
            AttendanceResponseCode.ATTENDANCE_FIND_ALL_SUCCESS,
            attendanceUseCase.findAllDetailsByCurrentCardinal(userId),
        )
}
