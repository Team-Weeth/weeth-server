package com.weeth.domain.schedule.domain.service;

import jakarta.transaction.Transactional;
import com.weeth.domain.schedule.application.dto.request.ScheduleUpdateRequest;
import com.weeth.domain.schedule.domain.entity.Event;
import com.weeth.domain.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class EventUpdateService {

    public void update(Event event, ScheduleUpdateRequest dto, User user) {
        event.update(dto.getTitle(), dto.getContent(), dto.getLocation(), dto.getStart(), dto.getEnd(), user);
    }
}
