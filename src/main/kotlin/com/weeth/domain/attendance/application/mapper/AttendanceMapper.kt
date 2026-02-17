package com.weeth.domain.attendance.application.mapper;

import com.weeth.domain.attendance.application.dto.AttendanceDTO;
import com.weeth.domain.attendance.domain.entity.Attendance;
import com.weeth.domain.user.domain.entity.User;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AttendanceMapper {

    @Mappings({
            @Mapping(target = "attendanceRate", source = "user.attendanceRate"),
            @Mapping(target = "title", source = "attendance.meeting.title"),
            @Mapping(target = "status", source = "attendance.status"),
            @Mapping(target = "code", ignore = true),
            @Mapping(target = "start", source = "attendance.meeting.start"),
            @Mapping(target = "end", source = "attendance.meeting.end"),
            @Mapping(target = "location", source = "attendance.meeting.location"),
    })
    AttendanceDTO.Main toMainDto(User user, Attendance attendance);

    @Mappings({
            @Mapping(target = "attendanceRate", source = "user.attendanceRate"),
            @Mapping(target = "title", source = "attendance.meeting.title"),
            @Mapping(target = "status", source = "attendance.status"),
            @Mapping(target = "code", source = "attendance.meeting.code"),
            @Mapping(target = "start", source = "attendance.meeting.start"),
            @Mapping(target = "end", source = "attendance.meeting.end"),
            @Mapping(target = "location", source = "attendance.meeting.location"),
    })
    AttendanceDTO.Main toAdminResponse(User user, Attendance attendance);

    @Mappings({
            @Mapping(target = "attendances", source = "attendances"),
            @Mapping(target = "total", expression = "java( user.getAttendanceCount() + user.getAbsenceCount() )")
    })
    AttendanceDTO.Detail toDetailDto(User user, List<AttendanceDTO.Response> attendances);

    @Mappings({
            @Mapping(target = "title", source = "attendance.meeting.title"),
            @Mapping(target = "start", source = "attendance.meeting.start"),
            @Mapping(target = "end", source = "attendance.meeting.end"),
            @Mapping(target = "location", source = "attendance.meeting.location"),
    })    AttendanceDTO.Response toResponseDto(Attendance attendance);

    @Mappings({
            @Mapping(target = "id", source = "attendance.id"),
            @Mapping(target = "status", source = "attendance.status"),
            @Mapping(target = "name", source = "attendance.user.name"),
            @Mapping(target = "position", source = "attendance.user.position"),
            @Mapping(target = "department", source = "attendance.user.department"),
            @Mapping(target = "studentId", source = "attendance.user.studentId")
    })
    AttendanceDTO.AttendanceInfo toAttendanceInfoDto(Attendance attendance);

}
