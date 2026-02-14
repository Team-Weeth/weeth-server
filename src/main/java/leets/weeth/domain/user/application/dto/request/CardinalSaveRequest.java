package leets.weeth.domain.user.application.dto.request;

import jakarta.validation.constraints.NotNull;

public record CardinalSaveRequest (
        @NotNull Integer cardinalNumber,
        @NotNull Integer year,
        @NotNull Integer semester,
        boolean inProgress
){
}
