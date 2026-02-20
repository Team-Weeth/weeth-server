package com.weeth.domain.attendance.presentation

import com.weeth.domain.attendance.application.dto.request.UpdateAttendanceStatusRequest
import com.weeth.domain.attendance.application.dto.response.AttendanceInfoResponse
import com.weeth.domain.attendance.application.exception.AttendanceErrorCode
import com.weeth.domain.attendance.application.usecase.command.ManageAttendanceUseCase
import com.weeth.domain.attendance.application.usecase.command.ManageSessionUseCase
import com.weeth.domain.attendance.application.usecase.query.GetAttendanceQueryService
import com.weeth.domain.schedule.application.dto.response.SessionInfosResponse
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@Tag(name = "ATTENDANCE ADMIN", description = "[ADMIN] 출석 어드민 API")
@RestController
@RequestMapping("/api/v1/admin/attendances")
@ApiErrorCodeExample(AttendanceErrorCode::class)
class AttendanceAdminController(
    private val manageAttendanceUseCase: ManageAttendanceUseCase,
    private val manageSessionUseCase: ManageSessionUseCase,
    private val getAttendanceQueryService: GetAttendanceQueryService,
) {
    @PatchMapping
    @Operation(summary = "출석 마감")
    fun close(
        @RequestParam now: LocalDate,
        @RequestParam cardinal: Int,
    ): CommonResponse<Void?> {
        manageAttendanceUseCase.close(now, cardinal)
        return CommonResponse.success(AttendanceResponseCode.ATTENDANCE_CLOSE_SUCCESS)
    }

    @GetMapping("/meetings")
    @Operation(summary = "정기모임 조회")
    fun getMeetings(
        @RequestParam(required = false) cardinal: Int?,
    ): CommonResponse<SessionInfosResponse> =
        CommonResponse.success(AttendanceResponseCode.MEETING_FIND_SUCCESS, manageSessionUseCase.findInfos(cardinal))

    @GetMapping("/{meetingId}")
    @Operation(summary = "모든 인원 정기모임 출석 정보 조회")
    fun getAllAttendance(
        @PathVariable meetingId: Long,
    ): CommonResponse<List<AttendanceInfoResponse>> =
        CommonResponse.success(
            AttendanceResponseCode.ATTENDANCE_FIND_DETAIL_SUCCESS,
            getAttendanceQueryService.findAllAttendanceByMeeting(meetingId),
        )

    @PatchMapping("/status")
    @Operation(summary = "모든 인원 정기모임 개별 출석 상태 수정")
    fun updateAttendanceStatus(
        @RequestBody @Valid attendanceUpdates: List<UpdateAttendanceStatusRequest>,
    ): CommonResponse<Void?> {
        manageAttendanceUseCase.updateStatus(attendanceUpdates)
        return CommonResponse.success(AttendanceResponseCode.ATTENDANCE_UPDATED_SUCCESS)
    }
}
