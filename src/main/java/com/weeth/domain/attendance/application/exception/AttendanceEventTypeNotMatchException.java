package com.weeth.domain.attendance.application.exception;

import com.weeth.global.common.exception.BaseException;

public class AttendanceEventTypeNotMatchException extends BaseException {
    public AttendanceEventTypeNotMatchException() {
        super(AttendanceErrorCode.ATTENDANCE_EVENT_TYPE_NOT_MATCH);
    }
}
