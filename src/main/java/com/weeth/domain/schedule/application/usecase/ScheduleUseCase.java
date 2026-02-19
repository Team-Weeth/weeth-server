package com.weeth.domain.schedule.application.usecase;

import com.weeth.domain.schedule.application.dto.response.ScheduleResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ScheduleUseCase {

    List<ScheduleResponse> findByMonthly(LocalDateTime start, LocalDateTime end);

    Map<Integer, List<ScheduleResponse>> findByYearly(Integer year, Integer semester);

}
