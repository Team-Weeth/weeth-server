package com.weeth.domain.schedule.application.mapper;

import com.weeth.domain.schedule.application.dto.ScheduleDTO;
import com.weeth.domain.schedule.domain.entity.Schedule;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ScheduleMapper {

    ScheduleDTO.Response toScheduleDTO(Schedule schedule, Boolean isMeeting);
}
