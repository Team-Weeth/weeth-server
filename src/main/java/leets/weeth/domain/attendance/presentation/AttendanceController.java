package leets.weeth.domain.attendance.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import leets.weeth.domain.attendance.application.dto.AttendanceDTO;
import leets.weeth.domain.attendance.application.exception.AttendanceErrorCode;
import leets.weeth.domain.attendance.application.usecase.AttendanceUseCase;
import leets.weeth.global.auth.annotation.CurrentUser;
import leets.weeth.domain.attendance.application.exception.AttendanceCodeMismatchException;
import leets.weeth.global.common.exception.ApiErrorCodeExample;
import leets.weeth.global.common.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import static leets.weeth.domain.attendance.application.dto.AttendanceDTO.*;
import static leets.weeth.domain.attendance.presentation.ResponseMessage.ATTENDANCE_CHECKIN_SUCCESS;
import static leets.weeth.domain.attendance.presentation.ResponseMessage.ATTENDANCE_FIND_ALL_SUCCESS;
import static leets.weeth.domain.attendance.presentation.ResponseMessage.ATTENDANCE_FIND_SUCCESS;

@Tag(name = "ATTENDANCE", description = "출석 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/attendances")
@ApiErrorCodeExample(AttendanceErrorCode.class)
public class AttendanceController {

    private final AttendanceUseCase attendanceUseCase;

    @PatchMapping
    @Operation(summary="출석체크")
    public CommonResponse<Void> checkIn(@Parameter(hidden = true) @CurrentUser Long userId, @RequestBody AttendanceDTO.CheckIn checkIn) throws AttendanceCodeMismatchException {
        attendanceUseCase.checkIn(userId, checkIn.code());
        return CommonResponse.createSuccess(ATTENDANCE_CHECKIN_SUCCESS.getMessage());
    }

    @GetMapping
    @Operation(summary="출석 메인페이지")
    public CommonResponse<Main> find(@Parameter(hidden = true) @CurrentUser Long userId) {
        return CommonResponse.createSuccess(ATTENDANCE_FIND_SUCCESS.getMessage(), attendanceUseCase.find(userId));
    }

    @GetMapping("/detail")
    @Operation(summary="출석 내역 상세조회")
    public CommonResponse<Detail> findAll(@Parameter(hidden = true) @CurrentUser Long userId) {
        return CommonResponse.createSuccess(ATTENDANCE_FIND_ALL_SUCCESS.getMessage(), attendanceUseCase.findAllDetailsByCurrentCardinal(userId));
    }
}
