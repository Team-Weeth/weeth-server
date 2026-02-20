package com.weeth.domain.schedule.domain.service;

import com.weeth.domain.schedule.application.dto.ScheduleDTO;
import com.weeth.domain.schedule.application.mapper.ScheduleMapper;
import com.weeth.domain.schedule.domain.entity.Meeting;
import com.weeth.domain.schedule.domain.entity.enums.MeetingStatus;
import com.weeth.domain.schedule.domain.repository.MeetingRepository;
import com.weeth.domain.schedule.application.exception.MeetingNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public Map<Integer, List<Meeting>> findByCardinals(List<Integer> cardinals) {
        if (cardinals == null || cardinals.isEmpty()) {
            return Map.of();
        }
        return meetingRepository.findAllByCardinalInOrderByCardinalAscStartAsc(cardinals).stream()
                .collect(Collectors.groupingBy(Meeting::getCardinal, LinkedHashMap::new, Collectors.toList()));
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
