package com.weeth.domain.attendance.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import com.weeth.domain.attendance.domain.entity.enums.Status;

import java.time.LocalDateTime;
import java.util.List;

public class AttendanceDTO {

    public record Main(
            Integer attendanceRate,
            String title,
            Status status,
            @Schema(description = "어드민인 경우 출석 코드 노출")
            Integer code,
            LocalDateTime start,
            LocalDateTime end,
            String location
    ) {}

    public record Detail(
            Integer attendanceCount,
            Integer total,
            Integer absenceCount,
            List<Response> attendances
    ) {}

    public record Response(
            Long id,
            Status status,
            String title,
            LocalDateTime start,
            LocalDateTime end,
            String location
    ) {}

    public record CheckIn(
            Integer code
    ) {}

    public record AttendanceInfo(
            Long id,
            Status status,
            String name,
            String position,
            String department,
            String studentId
    ) {}

    public record UpdateStatus(
            @NotNull Long attendanceId,
            @NotNull @Pattern(regexp = "ATTEND|ABSENT")String status
    ) {}
}
