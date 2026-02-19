package com.weeth.domain.schedule.domain.service;

import com.weeth.domain.attendance.domain.entity.Session;
import com.weeth.domain.attendance.domain.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MeetingSaveService {

    private final SessionRepository sessionRepository;

    public void save(Session session) {
        sessionRepository.save(session);
    }
}
