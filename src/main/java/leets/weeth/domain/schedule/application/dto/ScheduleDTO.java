package leets.weeth.domain.schedule.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import leets.weeth.domain.schedule.domain.entity.enums.Type;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class ScheduleDTO {

    public record Response(
            Long id,
            String title,
            LocalDateTime start,
            LocalDateTime end,
            Boolean isMeeting
    ) {}

    public record Time(
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {}

    public record Save(
            @NotBlank String title,
            @NotBlank String content,
            @NotBlank String location,
            String requiredItem,
            @NotNull Type type,
            @NotNull Integer cardinal,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {}

    public record Update(
            @NotBlank String title,
            @NotBlank String content,
            @NotBlank String location,
            String requiredItem,
            @NotNull Type type,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {}
}
