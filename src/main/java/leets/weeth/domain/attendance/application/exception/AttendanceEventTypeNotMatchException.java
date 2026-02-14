package leets.weeth.domain.attendance.application.exception;

import leets.weeth.global.common.exception.BusinessLogicException;

public class AttendanceEventTypeNotMatchException extends BusinessLogicException {
    public AttendanceEventTypeNotMatchException() {
        super(AttendanceErrorCode.ATTENDANCE_EVENT_TYPE_NOT_MATCH);
    }
}
