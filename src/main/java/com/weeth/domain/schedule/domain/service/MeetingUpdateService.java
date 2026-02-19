package com.weeth.domain.schedule.domain.service;

import com.weeth.domain.attendance.domain.entity.Session;
import com.weeth.domain.schedule.application.dto.ScheduleDTO;
import com.weeth.domain.user.domain.entity.User;
import org.springframework.stereotype.Service;

@Service
public class MeetingUpdateService {

    public void update(ScheduleDTO.Update dto, User user, Session session) {
        session.updateInfo(dto.title(), dto.content(), dto.location(), dto.requiredItem(), dto.start(), dto.end(), user);
    }
}
