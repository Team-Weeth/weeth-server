package com.weeth.domain.schedule.domain.service;

import com.weeth.domain.attendance.domain.entity.Session;
import com.weeth.domain.schedule.application.dto.request.ScheduleUpdateRequest;
import com.weeth.domain.user.domain.entity.User;
import org.springframework.stereotype.Service;

@Service
public class MeetingUpdateService {

    public void update(ScheduleUpdateRequest dto, User user, Session session) {
        session.updateInfo(dto.getTitle(), dto.getContent(), dto.getLocation(), dto.getStart(), dto.getEnd(), user);
    }
}
