package leets.weeth.domain.schedule.domain.service;

import leets.weeth.domain.schedule.domain.entity.Event;
import leets.weeth.domain.schedule.domain.repository.EventRepository;
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
