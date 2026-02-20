package com.weeth.domain.schedule.application.usecase;

import com.weeth.domain.schedule.application.dto.ScheduleDTO;
import com.weeth.domain.schedule.application.mapper.EventMapper;
import com.weeth.domain.schedule.domain.entity.Event;
import com.weeth.domain.schedule.domain.service.EventDeleteService;
import com.weeth.domain.schedule.domain.service.EventGetService;
import com.weeth.domain.schedule.domain.service.EventSaveService;
import com.weeth.domain.schedule.domain.service.EventUpdateService;
import com.weeth.domain.user.domain.entity.User;
import com.weeth.domain.user.domain.repository.CardinalReader;
import com.weeth.domain.user.domain.repository.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.weeth.domain.schedule.application.dto.EventDTO.Response;

@Service
@RequiredArgsConstructor
public class EventUseCaseImpl implements EventUseCase {

    private final UserReader userReader;
    private final EventGetService eventGetService;
    private final EventSaveService eventSaveService;
    private final EventUpdateService eventUpdateService;
    private final EventDeleteService eventDeleteService;
    private final CardinalReader cardinalReader;
    private final EventMapper mapper;

    @Override
    public Response find(Long eventId) {
        return mapper.to(eventGetService.find(eventId));
    }

    @Override
    @Transactional
    public void save(ScheduleDTO.Save dto, Long userId) {
        User user = userReader.getById(userId);
        cardinalReader.getByCardinalNumber(dto.cardinal());

        eventSaveService.save(mapper.from(dto, user));
    }

    @Override
    @Transactional
    public void update(Long eventId, ScheduleDTO.Update dto, Long userId) {
        User user = userReader.getById(userId);
        Event event = eventGetService.find(eventId);
        eventUpdateService.update(event, dto, user);
    }

    @Override
    @Transactional
    public void delete(Long eventId) {
        Event event = eventGetService.find(eventId);
        eventDeleteService.delete(event);
    }
}
