package com.weeth.domain.schedule.application.usecase;

import com.weeth.domain.schedule.application.dto.request.ScheduleSaveRequest;
import com.weeth.domain.schedule.application.dto.request.ScheduleUpdateRequest;
import com.weeth.domain.schedule.application.dto.response.SessionInfosResponse;
import com.weeth.domain.schedule.application.dto.response.SessionResponse;

public interface MeetingUseCase {

    SessionResponse find(Long userId, Long eventId);

    SessionInfosResponse find(Integer cardinal);

    void save(ScheduleSaveRequest dto, Long userId);

    void update(ScheduleUpdateRequest dto, Long userId, Long meetingId);

    void delete(Long meetingId);
}
