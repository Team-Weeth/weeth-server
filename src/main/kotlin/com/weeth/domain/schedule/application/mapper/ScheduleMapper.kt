package com.weeth.domain.schedule.application.mapper;

import com.weeth.domain.attendance.domain.entity.Session;
import com.weeth.domain.schedule.application.dto.ScheduleDTO;
import com.weeth.domain.schedule.domain.entity.Event;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ScheduleMapper {

    ScheduleDTO.Response toScheduleDTO(Event event, Boolean isMeeting);

    ScheduleDTO.Response toScheduleDTO(Session session, Boolean isMeeting);
}
