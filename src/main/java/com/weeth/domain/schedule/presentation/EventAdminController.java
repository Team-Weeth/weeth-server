package com.weeth.domain.schedule.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.weeth.domain.schedule.application.dto.request.ScheduleSaveRequest;
import com.weeth.domain.schedule.application.dto.request.ScheduleUpdateRequest;
import com.weeth.domain.schedule.application.exception.EventErrorCode;
import com.weeth.domain.schedule.application.usecase.EventUseCase;
import com.weeth.domain.schedule.application.usecase.MeetingUseCase;
import com.weeth.domain.schedule.domain.entity.enums.Type;
import com.weeth.global.auth.annotation.CurrentUser;
import com.weeth.global.common.exception.ApiErrorCodeExample;
import com.weeth.global.common.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import static com.weeth.domain.schedule.presentation.ScheduleResponseCode.*;

@Tag(name = "EVENT ADMIN", description = "[ADMIN] 일정 어드민 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/events")
@ApiErrorCodeExample(EventErrorCode.class)
public class EventAdminController {

    private final EventUseCase eventUseCase;
    private final MeetingUseCase meetingUseCase;

    @PostMapping
    @Operation(summary = "일정/정기모임 생성")
    public CommonResponse<Void> save(@Valid @RequestBody ScheduleSaveRequest dto,
                                     @Parameter(hidden = true) @CurrentUser Long userId) {
        if (dto.getType() == Type.EVENT) {
            eventUseCase.save(dto, userId);
        } else {
            meetingUseCase.save(dto, userId);
        }

        return CommonResponse.success(EVENT_SAVE_SUCCESS);
    }

    @PatchMapping("/{eventId}")
    @Operation(summary = "일정 수정 (type은 변경할 수 없게 해주세요.)")
    public CommonResponse<Void> update(@PathVariable Long eventId, @Valid @RequestBody ScheduleUpdateRequest dto,
                                       @Parameter(hidden = true) @CurrentUser Long userId) {
        if (dto.getType() == Type.EVENT) {
            eventUseCase.update(eventId, dto, userId);
        } else {
            meetingUseCase.update(dto, userId, eventId);
        }

        return CommonResponse.success(EVENT_UPDATE_SUCCESS);
    }

    @DeleteMapping("/{eventId}")
    @Operation(summary = "일정 삭제")
    public CommonResponse<Void> delete(@PathVariable Long eventId) {
        eventUseCase.delete(eventId);

        return CommonResponse.success(EVENT_DELETE_SUCCESS);
    }
}
