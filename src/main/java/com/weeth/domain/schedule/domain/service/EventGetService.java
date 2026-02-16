package com.weeth.domain.schedule.domain.service;

import com.weeth.domain.schedule.application.dto.ScheduleDTO;
import com.weeth.domain.schedule.application.mapper.ScheduleMapper;
import com.weeth.domain.schedule.domain.entity.Event;
import com.weeth.domain.schedule.domain.repository.EventRepository;
import com.weeth.domain.schedule.application.exception.EventNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventGetService {

    private final EventRepository eventRepository;
    private final ScheduleMapper mapper;

    public Event find(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(EventNotFoundException::new);
    }

    public List<ScheduleDTO.Response> find(LocalDateTime start, LocalDateTime end) {
        return eventRepository.findByStartLessThanEqualAndEndGreaterThanEqualOrderByStartAsc(end, start).stream()
                .map(event -> mapper.toScheduleDTO(event, false))
                .toList();
    }

    public List<ScheduleDTO.Response> find(Integer cardinal) {
        return eventRepository.findAllByCardinal(cardinal).stream()
                .map(event -> mapper.toScheduleDTO(event, false))
                .toList();
    }
}
