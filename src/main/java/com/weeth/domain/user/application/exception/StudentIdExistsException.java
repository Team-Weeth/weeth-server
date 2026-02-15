package com.weeth.domain.user.application.exception;

import com.weeth.global.common.exception.BusinessLogicException;

public class StudentIdExistsException extends BusinessLogicException {
    public StudentIdExistsException() {
        super(UserErrorCode.STUDENT_ID_EXISTS);
    }
}
