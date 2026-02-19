package com.weeth.domain.schedule.domain.service;

import com.weeth.domain.attendance.domain.entity.Session;
import com.weeth.domain.attendance.domain.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MeetingDeleteService {

    private final SessionRepository sessionRepository;

    public void delete(Session session) {
        sessionRepository.delete(session);
    }
}
