package com.weeth.domain.attendance.application.exception;

import com.weeth.global.common.exception.BusinessLogicException;

public class AttendanceEventTypeNotMatchException extends BusinessLogicException {
    public AttendanceEventTypeNotMatchException() {
        super(AttendanceErrorCode.ATTENDANCE_EVENT_TYPE_NOT_MATCH);
    }
}
