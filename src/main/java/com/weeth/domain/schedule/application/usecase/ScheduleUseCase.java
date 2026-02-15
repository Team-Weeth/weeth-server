package com.weeth.domain.schedule.application.usecase;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.weeth.domain.schedule.application.dto.ScheduleDTO.Response;

public interface ScheduleUseCase {

    List<Response> findByMonthly(LocalDateTime start, LocalDateTime end);

    Map<Integer, List<Response>> findByYearly(Integer year, Integer semester);

}
