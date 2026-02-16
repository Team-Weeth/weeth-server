package com.weeth.domain.schedule.application.usecase;

import com.weeth.domain.schedule.application.dto.MeetingDTO;
import com.weeth.domain.schedule.application.dto.ScheduleDTO;

import java.util.List;

import static com.weeth.domain.schedule.application.dto.MeetingDTO.Info;
import static com.weeth.domain.schedule.application.dto.MeetingDTO.Response;

public interface MeetingUseCase {

    Response find(Long userId, Long eventId);

    MeetingDTO.Infos find(Integer cardinal);

    void save(ScheduleDTO.Save dto, Long userId);

    void update(ScheduleDTO.Update dto, Long userId, Long meetingId);

    void delete(Long meetingId);
}
