package com.weeth.domain.schedule.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.weeth.domain.schedule.application.exception.MeetingErrorCode;
import com.weeth.domain.attendance.application.usecase.command.ManageSessionUseCase;
import com.weeth.global.common.exception.ApiErrorCodeExample;
import com.weeth.global.common.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.weeth.domain.schedule.presentation.ScheduleResponseCode.MEETING_DELETE_SUCCESS;

@Tag(name = "MEETING ADMIN", description = "[ADMIN] 정기모임 어드민 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/meetings")
@ApiErrorCodeExample(MeetingErrorCode.class)
public class MeetingAdminController {

    private final ManageSessionUseCase manageSessionUseCase;

    @DeleteMapping("/{meetingId}")
    @Operation(summary = "정기모임 삭제")
    public CommonResponse<Void> delete(@PathVariable Long meetingId) {
        manageSessionUseCase.delete(meetingId);
        return CommonResponse.success(MEETING_DELETE_SUCCESS);
    }
}
