package com.weeth.domain.user.application.dto.response;

import com.weeth.domain.user.domain.entity.User;
import com.weeth.domain.user.domain.entity.UserCardinal;

import java.util.List;

public record UserCardinalDto(
        User user,
        List<UserCardinal> cardinals
) {
}
