package com.weeth.domain.schedule.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.weeth.domain.schedule.application.exception.EventErrorCode;
import com.weeth.domain.schedule.application.usecase.EventUseCase;
import com.weeth.global.common.exception.ApiErrorCodeExample;
import com.weeth.global.common.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.weeth.domain.schedule.application.dto.EventDTO.Response;
import static com.weeth.domain.schedule.presentation.ResponseMessage.EVENT_FIND_SUCCESS;

@Tag(name = "EVENT", description = "일정 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/events")
@ApiErrorCodeExample(EventErrorCode.class)
public class EventController {

    private final EventUseCase eventUseCase;

    @GetMapping("/{eventId}")
    @Operation(summary="일정 상세 조회")
    public CommonResponse<Response> find(@PathVariable Long eventId) {
        return CommonResponse.createSuccess(EVENT_FIND_SUCCESS.getMessage(),
                eventUseCase.find(eventId));
    }

}
