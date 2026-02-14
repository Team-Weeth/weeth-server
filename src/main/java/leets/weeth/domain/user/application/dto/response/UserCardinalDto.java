package leets.weeth.domain.user.application.dto.response;

import leets.weeth.domain.user.domain.entity.User;
import leets.weeth.domain.user.domain.entity.UserCardinal;

import java.util.List;

public record UserCardinalDto(
        User user,
        List<UserCardinal> cardinals
) {
}
