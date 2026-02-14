package leets.weeth.domain.schedule.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import leets.weeth.domain.schedule.application.dto.ScheduleDTO;
import leets.weeth.domain.schedule.application.exception.EventErrorCode;
import leets.weeth.domain.schedule.application.usecase.EventUseCase;
import leets.weeth.domain.schedule.application.usecase.MeetingUseCase;
import leets.weeth.domain.schedule.domain.entity.enums.Type;
import leets.weeth.global.auth.annotation.CurrentUser;
import leets.weeth.global.common.exception.ApiErrorCodeExample;
import leets.weeth.global.common.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import static leets.weeth.domain.schedule.presentation.ResponseMessage.*;

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
    public CommonResponse<Void> save(@Valid @RequestBody ScheduleDTO.Save dto,
                                     @Parameter(hidden = true) @CurrentUser Long userId) {
        if (dto.type() == Type.EVENT) {
            eventUseCase.save(dto, userId);
        } else {
            meetingUseCase.save(dto, userId);
        }

        return CommonResponse.createSuccess(EVENT_SAVE_SUCCESS.getMessage());
    }

    @PatchMapping("/{eventId}")
    @Operation(summary = "일정 수정 (type은 변경할 수 없게 해주세요.)")
    public CommonResponse<Void> update(@PathVariable Long eventId, @Valid @RequestBody ScheduleDTO.Update dto,
                                       @Parameter(hidden = true) @CurrentUser Long userId) {
        if (dto.type() == Type.EVENT) {
            eventUseCase.update(eventId, dto, userId);
        } else {
            meetingUseCase.update(dto, userId, eventId);
        }

        return CommonResponse.createSuccess(EVENT_UPDATE_SUCCESS.getMessage());
    }

    @DeleteMapping("/{eventId}")
    @Operation(summary = "일정 삭제")
    public CommonResponse<Void> delete(@PathVariable Long eventId) {
        eventUseCase.delete(eventId);

        return CommonResponse.createSuccess(EVENT_DELETE_SUCCESS.getMessage());
    }
}
