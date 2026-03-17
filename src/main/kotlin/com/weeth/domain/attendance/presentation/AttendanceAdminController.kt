package com.weeth.domain.attendance.presentation

import com.weeth.domain.attendance.application.dto.request.UpdateAttendanceStatusRequest
import com.weeth.domain.attendance.application.dto.response.AttendanceInfoResponse
import com.weeth.domain.attendance.application.dto.response.QrTokenResponse
import com.weeth.domain.attendance.application.exception.AttendanceErrorCode
import com.weeth.domain.attendance.application.usecase.command.GenerateQrTokenUseCase
import com.weeth.domain.attendance.application.usecase.command.ManageAttendanceUseCase
import com.weeth.domain.attendance.application.usecase.query.GetAttendanceQueryService
import com.weeth.domain.session.application.exception.SessionErrorCode
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import com.weeth.global.common.web.TsidParam
import com.weeth.global.common.web.TsidPathVariable
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@Tag(name = "ATTENDANCE ADMIN", description = "[ADMIN] 출석 어드민 API")
@RestController
@RequestMapping("/api/v4/admin/clubs/{clubId}/attendances")
@ApiErrorCodeExample(AttendanceErrorCode::class, SessionErrorCode::class)
class AttendanceAdminController(
    private val manageAttendanceUseCase: ManageAttendanceUseCase,
    private val getAttendanceQueryService: GetAttendanceQueryService,
    private val generateQrTokenUseCase: GenerateQrTokenUseCase,
) {
    @PatchMapping("/close")
    @Operation(summary = "출석 마감")
    fun close(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @RequestParam now: LocalDate,
        @RequestParam cardinal: Int,
    ): CommonResponse<Void?> {
        manageAttendanceUseCase.close(clubId, userId, now, cardinal)
        return CommonResponse.success(AttendanceResponseCode.ATTENDANCE_CLOSE_SUCCESS)
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "모든 인원 정기모임 출석 정보 조회")
    fun getAllAttendance(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @PathVariable sessionId: Long,
    ): CommonResponse<List<AttendanceInfoResponse>> =
        CommonResponse.success(
            AttendanceResponseCode.ATTENDANCE_FIND_DETAIL_SUCCESS,
            getAttendanceQueryService.findAllAttendanceBySession(clubId, userId, sessionId),
        )

    @PatchMapping("/status")
    @Operation(summary = "모든 인원 정기모임 개별 출석 상태 수정")
    fun updateAttendanceStatus(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @RequestBody @Valid attendanceUpdates: List<UpdateAttendanceStatusRequest>,
    ): CommonResponse<Void?> {
        manageAttendanceUseCase.updateStatus(clubId, userId, attendanceUpdates)
        return CommonResponse.success(AttendanceResponseCode.ATTENDANCE_UPDATED_SUCCESS)
    }

    @PostMapping("/{sessionId}/qr")
    @Operation(summary = "QR 코드 생성")
    fun generateQr(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable sessionId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<QrTokenResponse> =
        CommonResponse.success(
            AttendanceResponseCode.QR_TOKEN_GENERATE_SUCCESS,
            generateQrTokenUseCase.execute(sessionId, clubId, userId),
        )
}
