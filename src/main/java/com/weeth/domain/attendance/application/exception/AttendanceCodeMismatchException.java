package com.weeth.domain.attendance.application.exception;

import com.weeth.global.common.exception.BaseException;

public class AttendanceCodeMismatchException extends BaseException {
    public AttendanceCodeMismatchException() {
        super(AttendanceErrorCode.ATTENDANCE_CODE_MISMATCH);
    }
}
