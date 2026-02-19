package com.weeth.domain.schedule.application.usecase;

import com.weeth.domain.schedule.application.dto.request.ScheduleSaveRequest;
import com.weeth.domain.schedule.application.dto.request.ScheduleUpdateRequest;
import com.weeth.domain.schedule.application.dto.response.EventResponse;

public interface EventUseCase {

    EventResponse find(Long eventId);

    void save(ScheduleSaveRequest dto, Long userId);

    void update(Long eventId, ScheduleUpdateRequest dto, Long userId);

    void delete(Long eventId);
}
