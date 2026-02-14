package leets.weeth.domain.penalty.application.dto;

import jakarta.validation.constraints.NotNull;
import leets.weeth.domain.penalty.domain.entity.enums.PenaltyType;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public class PenaltyDTO {

    @Builder
    public record Save(
            @NotNull Long userId,
            @NotNull PenaltyType penaltyType,
            String penaltyDescription
    ){}

    @Builder
    public record Update(
            @NotNull Long penaltyId,
            String penaltyDescription
    ){}

    @Builder
    public record ResponseAll(
            Integer cardinal,
            List<Response> responses
    ){}

    @Builder
    public record Response(
        Long userId,
        Integer penaltyCount,
        Integer warningCount,
        String name,
        List<Integer> cardinals,
        List<PenaltyDTO.Penalties> Penalties
    ){}

    @Builder
    public record Penalties(
       Long penaltyId,
       PenaltyType penaltyType,
       Integer cardinal,
       String penaltyDescription,
       LocalDateTime time
    ){}

}

