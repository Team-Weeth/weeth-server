package leets.weeth.domain.schedule.domain.service;

import leets.weeth.domain.schedule.domain.entity.Meeting;
import leets.weeth.domain.schedule.domain.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MeetingDeleteService {

    private final MeetingRepository meetingRepository;

    public void delete(Meeting meeting) {
        meetingRepository.delete(meeting);
    }
}
