package leets.weeth.domain.attendance.application.exception;

import leets.weeth.global.common.exception.BusinessLogicException;

public class AttendanceNotFoundException extends BusinessLogicException {
    public AttendanceNotFoundException() {
        super(AttendanceErrorCode.ATTENDANCE_NOT_FOUND);
    }
}
