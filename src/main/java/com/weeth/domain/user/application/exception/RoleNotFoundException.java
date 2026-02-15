package com.weeth.domain.user.application.exception;

import com.weeth.global.common.exception.BusinessLogicException;

public class RoleNotFoundException extends BusinessLogicException {
    public RoleNotFoundException() {
        super(UserErrorCode.ROLE_NOT_FOUND);
    }
}
