package com.weeth.domain.user.application.exception;

import com.weeth.global.common.exception.BusinessLogicException;

public class DepartmentNotFoundException extends BusinessLogicException {
    public DepartmentNotFoundException() {
        super(UserErrorCode.DEPARTMENT_NOT_FOUND);
    }
}
