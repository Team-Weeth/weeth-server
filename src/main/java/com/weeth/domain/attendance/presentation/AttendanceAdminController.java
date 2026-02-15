package com.weeth.domain.attendance.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.weeth.domain.attendance.application.dto.AttendanceDTO;
import com.weeth.domain.attendance.application.exception.AttendanceErrorCode;
import com.weeth.domain.attendance.application.usecase.AttendanceUseCase;
import com.weeth.domain.schedule.application.dto.MeetingDTO;
import com.weeth.domain.schedule.application.usecase.MeetingUseCase;
import com.weeth.global.common.exception.ApiErrorCodeExample;
import com.weeth.global.common.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static com.weeth.domain.attendance.presentation.ResponseMessage.*;

@Tag(name = "ATTENDANCE ADMIN", description = "[ADMIN] 출석 어드민 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/attendances")
@ApiErrorCodeExample(AttendanceErrorCode.class)
public class AttendanceAdminController {

    private final AttendanceUseCase attendanceUseCase;
    private final MeetingUseCase meetingUseCase;

    @PatchMapping
    @Operation(summary="출석 마감")
    public CommonResponse<Void> close(@RequestParam LocalDate now, @RequestParam Integer cardinal) {
        attendanceUseCase.close(now, cardinal);
        return CommonResponse.createSuccess(ATTENDANCE_CLOSE_SUCCESS.getMessage());
    }

    @GetMapping("/meetings")
    @Operation(summary = "정기모임 조회")
    public CommonResponse<MeetingDTO.Infos> getMeetings(@RequestParam(required = false) Integer cardinal) {
        MeetingDTO.Infos response = meetingUseCase.find(cardinal);

        return CommonResponse.createSuccess(MEETING_FIND_SUCCESS.getMessage(), response);
    }

    @GetMapping("/{meetingId}")
    @Operation(summary = "모든 인원 정기모임 출석 정보 조회")
    public CommonResponse<List<AttendanceDTO.AttendanceInfo>> getAllAttendance(@PathVariable Long meetingId) {
        return CommonResponse.createSuccess(ATTENDANCE_FIND_DETAIL_SUCCESS.getMessage(), attendanceUseCase.findAllAttendanceByMeeting(meetingId));
    }

    @PatchMapping("/status")
    @Operation(summary = "모든 인원 정기모임 개별 출석 상태 수정")
    public CommonResponse<Void> updateAttendanceStatus(@RequestBody @Valid List<AttendanceDTO.UpdateStatus> attendanceUpdates) {
        attendanceUseCase.updateAttendanceStatus(attendanceUpdates);
        return CommonResponse.createSuccess(ATTENDANCE_UPDATED_SUCCESS.getMessage());
    }
}
