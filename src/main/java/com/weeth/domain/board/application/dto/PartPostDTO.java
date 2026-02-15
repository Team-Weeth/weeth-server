package com.weeth.domain.board.application.dto;

import jakarta.validation.constraints.NotNull;
import com.weeth.domain.board.domain.entity.enums.Category;
import com.weeth.domain.board.domain.entity.enums.Part;

public record PartPostDTO(
        @NotNull Part part,
        @NotNull Category category,
        Integer cardinalNumber,
        Integer week,
        String studyName
) {
}
