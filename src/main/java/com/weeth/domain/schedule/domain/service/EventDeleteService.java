package com.weeth.domain.schedule.domain.service;

import com.weeth.domain.schedule.domain.entity.Event;
import com.weeth.domain.schedule.domain.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventDeleteService {

    private final EventRepository eventRepository;

    public void delete(Event event) {
        eventRepository.delete(event);
    }
}
