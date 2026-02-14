package leets.weeth.domain.schedule.domain.service;

import leets.weeth.domain.schedule.domain.entity.Meeting;
import leets.weeth.domain.schedule.domain.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MeetingSaveService {

    private final MeetingRepository meetingRepository;

    public void save(Meeting meeting) {
        meetingRepository.save(meeting);
    }
}
