package com.weeth.domain.user.application.exception;

import com.weeth.global.common.exception.BaseException;

public class DepartmentNotFoundException extends BaseException {
    public DepartmentNotFoundException() {
        super(UserErrorCode.DEPARTMENT_NOT_FOUND);
    }
}
