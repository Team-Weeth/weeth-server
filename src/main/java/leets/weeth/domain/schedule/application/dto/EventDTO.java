package leets.weeth.domain.schedule.application.dto;

import leets.weeth.domain.schedule.domain.entity.enums.Type;

import java.time.LocalDateTime;

public class EventDTO {

    public record Response(
            Long id,
            String title,
            String content,
            String location,
            String requiredItem,
            String name,
            Integer cardinal,
            Type type,
            LocalDateTime start,
            LocalDateTime end,
            LocalDateTime createdAt,
            LocalDateTime modifiedAt
    ) {}
}

