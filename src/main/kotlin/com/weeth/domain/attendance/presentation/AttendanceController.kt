package com.weeth.domain.attendance.presentation

import com.weeth.domain.attendance.application.dto.request.CheckInRequest
import com.weeth.domain.attendance.application.dto.response.AttendanceDetailResponse
import com.weeth.domain.attendance.application.dto.response.AttendanceSummaryResponse
import com.weeth.domain.attendance.application.exception.AttendanceErrorCode
import com.weeth.domain.attendance.application.usecase.command.ManageAttendanceUseCase
import com.weeth.domain.attendance.application.usecase.command.SubscribeAttendanceSseUseCase
import com.weeth.domain.attendance.application.usecase.query.GetAttendanceQueryService
import com.weeth.domain.cardinal.application.exception.CardinalErrorCode
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import com.weeth.global.common.web.TsidParam
import com.weeth.global.common.web.TsidPathVariable
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Tag(name = "ATTENDANCE", description = "출석 API")
@RestController
@RequestMapping("/api/v4/clubs/{clubId}/attendances")
@ApiErrorCodeExample(AttendanceErrorCode::class)
class AttendanceController(
    private val manageAttendanceUseCase: ManageAttendanceUseCase,
    private val getAttendanceQueryService: GetAttendanceQueryService,
    private val subscribeAttendanceSseUseCase: SubscribeAttendanceSseUseCase,
) {
    @PostMapping("/sessions/{sessionId}/check-in")
    @Operation(summary = "출석체크")
    fun checkIn(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable sessionId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @RequestBody checkIn: CheckInRequest,
    ): CommonResponse<Void?> {
        manageAttendanceUseCase.checkIn(clubId, userId, sessionId, checkIn.code)
        return CommonResponse.success(AttendanceResponseCode.ATTENDANCE_CHECKIN_SUCCESS)
    }

    @GetMapping
    @Operation(
        summary = "내 출석 요약 조회",
        description = """
            출석을 진행하기 전 오늘의 출석 유무를 확인하기 위해서 사용됩니다.(대시보드, 출석 페이지).
            cardinalNumber를 지정하면 해당 기수의 출석률과 오늘 출석만 반환합니다.
            생략하면 기존 전체 기수 누적 출석률과 오늘 출석을 반환합니다.
            미결은 출석률 분모에서 제외하며, 확정 기록이 없으면 0입니다.
            오늘의 출석이 없는 경우 status를 포함한 세션 필드는 null로 반환됩니다.
            """,
    )
    @ApiErrorCodeExample(AttendanceErrorCode::class, CardinalErrorCode::class)
    fun find(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @Parameter(description = "기수 번호 (기수 ID가 아님). 생략 시 전체 기수 누적 요약")
        @RequestParam(required = false) cardinalNumber: Int? = null,
    ): CommonResponse<AttendanceSummaryResponse> =
        CommonResponse.success(
            AttendanceResponseCode.ATTENDANCE_FIND_SUCCESS,
            getAttendanceQueryService.findAttendance(clubId, userId, cardinalNumber),
        )

    @GetMapping("/detail")
    @Operation(
        summary = "내 기수별 출석 상세 내역 조회",
        description = """
            cardinalNumber 생략 시 본인 최신 소속 기수입니다.
            기록과 통계는 동일 기수 기준이며 미결은 total/출석률 분모에서 제외합니다.
            동아리에 존재하는 기수에 내 기록이 없으면 빈 목록과 0 통계를 반환합니다.
        """,
    )
    @ApiErrorCodeExample(AttendanceErrorCode::class, CardinalErrorCode::class)
    fun findAll(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @Parameter(description = "기수 번호 (기수 ID가 아님). 생략 시 본인 최신 소속 기수")
        @RequestParam(required = false) cardinalNumber: Int? = null,
    ): CommonResponse<AttendanceDetailResponse> =
        CommonResponse.success(
            AttendanceResponseCode.ATTENDANCE_FIND_ALL_SUCCESS,
            getAttendanceQueryService.findAllDetailsByCurrentCardinal(clubId, userId, cardinalNumber),
        )

    @GetMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    @Operation(summary = "출석 SSE 구독")
    fun subscribe(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): SseEmitter = subscribeAttendanceSseUseCase.execute(clubId, userId)
}
