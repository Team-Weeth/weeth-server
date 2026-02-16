package com.weeth.domain.schedule.domain.service;

import com.weeth.domain.schedule.application.dto.ScheduleDTO;
import com.weeth.domain.schedule.domain.entity.Meeting;
import com.weeth.domain.user.domain.entity.User;
import org.springframework.stereotype.Service;

@Service
public class MeetingUpdateService {

    public void update(ScheduleDTO.Update dto, User user, Meeting meeting) {
        meeting.update(dto, user);
    }
}
