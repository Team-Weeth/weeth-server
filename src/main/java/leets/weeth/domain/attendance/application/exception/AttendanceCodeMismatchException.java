package leets.weeth.domain.attendance.application.exception;

import leets.weeth.global.common.exception.BusinessLogicException;

public class AttendanceCodeMismatchException extends BusinessLogicException {
    public AttendanceCodeMismatchException() {
        super(AttendanceErrorCode.ATTENDANCE_CODE_MISMATCH);
    }
}
