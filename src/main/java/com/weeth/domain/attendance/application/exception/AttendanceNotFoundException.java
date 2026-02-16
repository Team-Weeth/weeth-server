package com.weeth.domain.attendance.application.exception;

import com.weeth.global.common.exception.BaseException;

public class AttendanceNotFoundException extends BaseException {
    public AttendanceNotFoundException() {
        super(AttendanceErrorCode.ATTENDANCE_NOT_FOUND);
    }
}
