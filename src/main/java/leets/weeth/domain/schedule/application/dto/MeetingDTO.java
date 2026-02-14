package leets.weeth.domain.schedule.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import leets.weeth.domain.schedule.domain.entity.enums.Type;

import java.time.LocalDateTime;
import java.util.List;

public class MeetingDTO {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Response(
            Long id,
            String title,
            String content,
            String location,
            String requiredItem,
            String name,
            Integer cardinal,
            Type type,
            Integer code,
            LocalDateTime start,
            LocalDateTime end,
            LocalDateTime createdAt,
            LocalDateTime modifiedAt
    ) {}

    public record Info(
            Long id,
            Integer cardinal,
            String title,
            LocalDateTime start
    ) {}

    public record Infos(
        Info thisWeek,
        List<Info> meetings
    ) {}


}
