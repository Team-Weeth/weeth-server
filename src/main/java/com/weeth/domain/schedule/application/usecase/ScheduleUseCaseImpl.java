package com.weeth.domain.schedule.application.usecase;

import com.weeth.domain.schedule.application.dto.response.ScheduleResponse;
import com.weeth.domain.schedule.domain.service.EventGetService;
import com.weeth.domain.schedule.domain.service.MeetingGetService;
import com.weeth.domain.user.domain.entity.Cardinal;
import com.weeth.domain.user.domain.service.CardinalGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ScheduleUseCaseImpl implements ScheduleUseCase {

    private final EventGetService eventGetService;
    private final MeetingGetService meetingGetService;
    private final CardinalGetService cardinalGetService;

    @Override
    public List<ScheduleResponse> findByMonthly(LocalDateTime start, LocalDateTime end) {
        List<ScheduleResponse> events = eventGetService.find(start, end);
        List<ScheduleResponse> meetings = meetingGetService.find(start, end);

        return Stream.of(events, meetings)
                .flatMap(Collection::stream)
                .sorted(Comparator.comparing(ScheduleResponse::getStart))
                .toList();
    }

    @Override
    public Map<Integer, List<ScheduleResponse>> findByYearly(Integer year, Integer semester) {
        Cardinal cardinal = cardinalGetService.find(year, semester);

        List<ScheduleResponse> events = eventGetService.find(cardinal.getCardinalNumber());
        List<ScheduleResponse> meetings = meetingGetService.findByCardinal(cardinal.getCardinalNumber());

        return Stream.of(events, meetings)
                .flatMap(Collection::stream)
                .sorted(Comparator.comparing(ScheduleResponse::getStart))
                .flatMap(schedule -> {
                    List<Map.Entry<Integer, ScheduleResponse>> monthEventPairs = new ArrayList<>();

                    int left = schedule.getStart().getMonthValue();
                    int right = schedule.getEnd().getMonthValue() + 1;
                    IntStream.range(left, right)
                            .forEach(month -> monthEventPairs.add(
                                    new AbstractMap.SimpleEntry<>(month, schedule))
                            );

                    return monthEventPairs.stream();
                })
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));
    }
}
