package leets.weeth.domain.user.application.mapper;

import leets.weeth.domain.user.application.dto.request.CardinalSaveRequest;
import leets.weeth.domain.user.application.dto.response.CardinalResponse;
import leets.weeth.domain.user.application.dto.response.UserCardinalDto;
import leets.weeth.domain.user.domain.entity.Cardinal;
import leets.weeth.domain.user.domain.entity.User;
import leets.weeth.domain.user.domain.entity.UserCardinal;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CardinalMapper {

    Cardinal from(CardinalSaveRequest dto);

    CardinalResponse to(Cardinal cardinal);

    UserCardinalDto toUserCardinalDto(User user, List<UserCardinal> cardinals);
}
