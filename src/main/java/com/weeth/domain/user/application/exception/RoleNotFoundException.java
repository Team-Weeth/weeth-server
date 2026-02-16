package com.weeth.domain.user.application.exception;

import com.weeth.global.common.exception.BaseException;

public class RoleNotFoundException extends BaseException {
    public RoleNotFoundException() {
        super(UserErrorCode.ROLE_NOT_FOUND);
    }
}
