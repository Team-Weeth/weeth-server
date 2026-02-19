package com.weeth.domain.schedule.domain.service;

import com.weeth.domain.attendance.domain.entity.Session;
import com.weeth.domain.attendance.domain.entity.enums.SessionStatus;
import com.weeth.domain.attendance.domain.repository.SessionRepository;
import com.weeth.domain.schedule.application.dto.response.ScheduleResponse;
import com.weeth.domain.schedule.application.mapper.ScheduleMapper;
import com.weeth.domain.schedule.application.exception.MeetingNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingGetService {

    private final SessionRepository sessionRepository;
    private final ScheduleMapper mapper;

    public Session find(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(MeetingNotFoundException::new);
    }

    public List<ScheduleResponse> find(LocalDateTime start, LocalDateTime end) {
        return sessionRepository.findByStartLessThanEqualAndEndGreaterThanEqualOrderByStartAsc(end, start).stream()
                .map(session -> mapper.toResponse(session, true))
                .toList();
    }

    public List<Session> find(Integer cardinal) {
        return sessionRepository.findAllByCardinalOrderByStartAsc(cardinal);
    }

    public List<Session> findMeetingByCardinal(Integer cardinal) {
        return sessionRepository.findAllByCardinalOrderByStartDesc(cardinal);
    }

    public List<Session> findAll() {
        return sessionRepository.findAllByOrderByStartDesc();
    }

    public List<ScheduleResponse> findByCardinal(Integer cardinal) {
        return sessionRepository.findAllByCardinal(cardinal).stream()
                .map(session -> mapper.toResponse(session, true))
                .toList();
    }

    public List<Session> findAllOpenMeetingsBeforeNow() {
        return sessionRepository.findAllByStatusAndEndBeforeOrderByEndAsc(SessionStatus.OPEN, LocalDateTime.now());
    }
}
