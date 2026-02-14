package leets.weeth.domain.schedule.application.usecase;

import leets.weeth.domain.schedule.domain.service.EventGetService;
import leets.weeth.domain.schedule.domain.service.MeetingGetService;
import leets.weeth.domain.user.domain.entity.Cardinal;
import leets.weeth.domain.user.domain.service.CardinalGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static leets.weeth.domain.schedule.application.dto.ScheduleDTO.Response;

@Service
@RequiredArgsConstructor
public class ScheduleUseCaseImpl implements ScheduleUseCase {

    private final EventGetService eventGetService;
    private final MeetingGetService meetingGetService;
    private final CardinalGetService cardinalGetService;

    @Override
    public List<Response> findByMonthly(LocalDateTime start, LocalDateTime end) {
        List<Response> events = eventGetService.find(start, end);
        List<Response> meetings = meetingGetService.find(start, end);

        return Stream.of(events, meetings)
                .flatMap(Collection::stream)
                .sorted(Comparator.comparing(Response::start))
                .toList();
    }

    @Override
    public Map<Integer, List<Response>> findByYearly(Integer year, Integer semester) {
        Cardinal cardinal = cardinalGetService.find(year, semester);

        List<Response> events = eventGetService.find(cardinal.getCardinalNumber());
        List<Response> meetings = meetingGetService.findByCardinal(cardinal.getCardinalNumber());

        return Stream.of(events, meetings)
                .flatMap(Collection::stream)    // 병합
                .sorted(Comparator.comparing(Response::start))  // 스케줄 시작 시간으로 정렬
                .flatMap(schedule -> {
                    List<Map.Entry<Integer, Response>> monthEventPairs = new ArrayList<>();

                    int left = schedule.start().getMonthValue();
                    int right = schedule.end().getMonthValue() + 1;
                    IntStream.range(left, right)    // 기간 내 포함된 달 계산
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
