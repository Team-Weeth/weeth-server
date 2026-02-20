package com.weeth.domain.schedule.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.weeth.domain.schedule.application.dto.response.SessionResponse;
import com.weeth.domain.schedule.application.exception.MeetingErrorCode;
import com.weeth.domain.attendance.application.usecase.command.ManageSessionUseCase;
import com.weeth.global.auth.annotation.CurrentUser;
import com.weeth.global.common.exception.ApiErrorCodeExample;
import com.weeth.global.common.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.weeth.domain.schedule.presentation.ScheduleResponseCode.MEETING_FIND_SUCCESS;

@Tag(name = "MEETING", description = "정기모임 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/meetings")
@ApiErrorCodeExample(MeetingErrorCode.class)
public class MeetingController {

    private final ManageSessionUseCase manageSessionUseCase;

    @GetMapping("/{meetingId}")
    @Operation(summary="정기모임 상세 조회")
    public CommonResponse<SessionResponse> find(@Parameter(hidden = true) @CurrentUser Long userId,
                                                @PathVariable Long meetingId) {
        return CommonResponse.success(MEETING_FIND_SUCCESS, manageSessionUseCase.find(userId, meetingId));
    }
}
