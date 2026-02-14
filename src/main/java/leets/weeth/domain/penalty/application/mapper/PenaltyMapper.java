package leets.weeth.domain.penalty.application.mapper;

import leets.weeth.domain.penalty.application.dto.PenaltyDTO;
import leets.weeth.domain.penalty.domain.entity.Penalty;
import leets.weeth.domain.penalty.domain.entity.enums.PenaltyType;
import leets.weeth.domain.user.domain.entity.Cardinal;
import leets.weeth.domain.user.domain.entity.User;
import leets.weeth.domain.user.domain.entity.UserCardinal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PenaltyMapper {

    @Mapping(target = "user", source = "user")
    @Mapping(target = "cardinal", source = "cardinal")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    Penalty fromPenaltyDto(PenaltyDTO.Save dto, User user, Cardinal cardinal);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    Penalty toAutoPenalty(String penaltyDescription, User user, Cardinal cardinal, PenaltyType penaltyType);

    @Mapping(target = "Penalties", source = "penalties")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "cardinals", expression = "java( toCardinalNumbers(userCardinals) )")
    PenaltyDTO.Response toPenaltyDto(User user, List<PenaltyDTO.Penalties> penalties, List<UserCardinal> userCardinals);

    @Mapping(target = "time", source = "modifiedAt")
    @Mapping(target = "penaltyId", source = "id")
    @Mapping(target = "cardinal",
            expression = "java(penalty.getCardinal() != null ? penalty.getCardinal().getCardinalNumber() : null)")

    PenaltyDTO.Penalties toPenalties(Penalty penalty);

    PenaltyDTO.ResponseAll toResponseAll(Integer cardinal, List<PenaltyDTO.Response> responses);

    default List<Integer> toCardinalNumbers(List<UserCardinal> userCardinals) {
        if (userCardinals == null || userCardinals.isEmpty()) {
            return Collections.emptyList();
        }

        return userCardinals.stream()
                .map(uc -> uc.getCardinal().getCardinalNumber())
                .collect(Collectors.toList());
    }

}
