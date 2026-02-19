package com.weeth.domain.schedule.application.usecase;

import com.weeth.domain.schedule.application.dto.request.ScheduleSaveRequest;
import com.weeth.domain.schedule.application.dto.request.ScheduleUpdateRequest;
import com.weeth.domain.schedule.application.dto.response.EventResponse;
import com.weeth.domain.schedule.application.mapper.EventMapper;
import com.weeth.domain.schedule.domain.entity.Event;
import com.weeth.domain.schedule.domain.service.EventDeleteService;
import com.weeth.domain.schedule.domain.service.EventGetService;
import com.weeth.domain.schedule.domain.service.EventSaveService;
import com.weeth.domain.schedule.domain.service.EventUpdateService;
import com.weeth.domain.user.domain.entity.User;
import com.weeth.domain.user.domain.service.CardinalGetService;
import com.weeth.domain.user.domain.service.UserGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventUseCaseImpl implements EventUseCase {

    private final UserGetService userGetService;
    private final EventGetService eventGetService;
    private final EventSaveService eventSaveService;
    private final EventUpdateService eventUpdateService;
    private final EventDeleteService eventDeleteService;
    private final CardinalGetService cardinalGetService;
    private final EventMapper mapper;

    @Override
    public EventResponse find(Long eventId) {
        return mapper.toResponse(eventGetService.find(eventId));
    }

    @Override
    @Transactional
    public void save(ScheduleSaveRequest dto, Long userId) {
        User user = userGetService.find(userId);
        cardinalGetService.findByUserSide(dto.getCardinal());

        eventSaveService.save(mapper.toEntity(dto, user));
    }

    @Override
    @Transactional
    public void update(Long eventId, ScheduleUpdateRequest dto, Long userId) {
        User user = userGetService.find(userId);
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
