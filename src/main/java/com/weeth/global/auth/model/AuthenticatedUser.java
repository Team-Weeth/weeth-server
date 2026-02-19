package com.weeth.global.auth.model;

import com.weeth.domain.user.domain.entity.enums.Role;

public record AuthenticatedUser(
        Long id,
        String email,
        Role role
) {
}
