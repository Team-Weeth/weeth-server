package com.weeth.domain.schedule.application.mapper;

import com.weeth.domain.attendance.domain.entity.Session;
import com.weeth.domain.schedule.application.dto.ScheduleDTO;
import com.weeth.domain.user.domain.entity.User;
import org.mapstruct.*;

import java.util.Random;

import static com.weeth.domain.schedule.application.dto.MeetingDTO.Info;
import static com.weeth.domain.schedule.application.dto.MeetingDTO.Response;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MeetingMapper {

    @Mapping(target = "name", source = "user.name")
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "type", expression = "java(Type.MEETING)")
    Response to(Session session);

    Info toInfo(Session session);

    @Mapping(target = "name", source = "user.name")
    @Mapping(target = "type", expression = "java(Type.MEETING)")
    Response toAdminResponse(Session session);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "code", expression = "java( generateCode() )"),
            @Mapping(target = "status", ignore = true),
            @Mapping(target = "user", source = "user")
    })
    Session from(ScheduleDTO.Save dto, User user);

    default Integer generateCode() {
        return new Random().nextInt(9000) + 1000;
    }
}
