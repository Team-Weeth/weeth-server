package com.weeth.domain.user.application.exception;

import com.weeth.global.common.exception.BaseException;

public class StudentIdExistsException extends BaseException {
    public StudentIdExistsException() {
        super(UserErrorCode.STUDENT_ID_EXISTS);
    }
}
