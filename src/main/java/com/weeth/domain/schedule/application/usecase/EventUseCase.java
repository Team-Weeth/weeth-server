package com.weeth.domain.schedule.application.usecase;

import com.weeth.domain.schedule.application.dto.ScheduleDTO;

import static com.weeth.domain.schedule.application.dto.EventDTO.*;

public interface EventUseCase {

    Response find(Long eventId);

    void save(ScheduleDTO.Save dto, Long userId);

    void update(Long eventId, ScheduleDTO.Update dto, Long userId);

    void delete(Long eventId);
}
