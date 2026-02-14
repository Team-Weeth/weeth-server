package leets.weeth.domain.schedule.domain.service;

import leets.weeth.domain.schedule.application.dto.ScheduleDTO;
import leets.weeth.domain.schedule.application.mapper.ScheduleMapper;
import leets.weeth.domain.schedule.domain.entity.Meeting;
import leets.weeth.domain.schedule.domain.entity.enums.MeetingStatus;
import leets.weeth.domain.schedule.domain.repository.MeetingRepository;
import leets.weeth.domain.schedule.application.exception.MeetingNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingGetService {

    private final MeetingRepository meetingRepository;
    private final ScheduleMapper mapper;

    public Meeting find(Long meetingId) {
        return meetingRepository.findById(meetingId)
                .orElseThrow(MeetingNotFoundException::new);
    }

    public List<ScheduleDTO.Response> find(LocalDateTime start, LocalDateTime end) {
        return meetingRepository.findByStartLessThanEqualAndEndGreaterThanEqualOrderByStartAsc(end, start).stream()
                .map(meeting -> mapper.toScheduleDTO(meeting, true))
                .toList();
    }

    public List<Meeting> find(Integer cardinal) {
        return meetingRepository.findAllByCardinalOrderByStartAsc(cardinal);
    }

    public List<Meeting> findMeetingByCardinal(Integer cardinal) {
        return meetingRepository.findAllByCardinalOrderByStartDesc(cardinal);
    }

    public List<Meeting> findAll() {
        return meetingRepository.findAllByOrderByStartDesc();
    }

    public List<ScheduleDTO.Response> findByCardinal(Integer cardinal) {
        return meetingRepository.findAllByCardinal(cardinal).stream()
                .map(meeting -> mapper.toScheduleDTO(meeting, true))
                .toList();
    }

    public List<Meeting> findAllOpenMeetingsBeforeNow() {
        return meetingRepository.findAllByMeetingStatusAndEndBeforeOrderByEndAsc(MeetingStatus.OPEN, LocalDateTime.now());
    }
}
