package leets.weeth.domain.user.application.dto.request;

import jakarta.validation.constraints.NotNull;

public record CardinalUpdateRequest(
        @NotNull Long id,
        @NotNull Integer year,
        @NotNull Integer semester,
        boolean inProgress
) {
}
