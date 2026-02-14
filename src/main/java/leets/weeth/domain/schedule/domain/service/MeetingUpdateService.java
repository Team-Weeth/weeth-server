package leets.weeth.domain.schedule.domain.service;

import leets.weeth.domain.schedule.application.dto.ScheduleDTO;
import leets.weeth.domain.schedule.domain.entity.Meeting;
import leets.weeth.domain.user.domain.entity.User;
import org.springframework.stereotype.Service;

@Service
public class MeetingUpdateService {

    public void update(ScheduleDTO.Update dto, User user, Meeting meeting) {
        meeting.update(dto, user);
    }
}
