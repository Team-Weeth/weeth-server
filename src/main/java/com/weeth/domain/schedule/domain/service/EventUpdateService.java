package com.weeth.domain.schedule.domain.service;

import jakarta.transaction.Transactional;
import com.weeth.domain.schedule.application.dto.ScheduleDTO;
import com.weeth.domain.schedule.domain.entity.Event;
import com.weeth.domain.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class EventUpdateService {

    public void update(Event event, ScheduleDTO.Update dto, User user) {
        event.update(dto.title(), dto.content(), dto.location(), dto.requiredItem(), dto.start(), dto.end(), user);
    }
}
