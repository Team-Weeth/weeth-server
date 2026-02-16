package com.weeth.domain.user.application.dto.response;

import com.weeth.domain.user.domain.entity.enums.CardinalStatus;

import java.time.LocalDateTime;

public record CardinalResponse(
        Long id,
        Integer cardinalNumber,
        Integer year,
        Integer semester,
        CardinalStatus status,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {
}
